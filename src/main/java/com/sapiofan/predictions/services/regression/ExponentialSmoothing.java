package com.sapiofan.predictions.services.regression;

import com.sapiofan.predictions.entities.Data;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ExponentialSmoothing {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private static final Logger log = LoggerFactory.getLogger(ExponentialSmoothing.class);

    private final int SEASONAL_PERIOD = 7;
    private int EXISTED_PERIOD_AFTER_SEASONAL;
    private final int SEASONAL_REPETITION = 8;

    private double ALPHA = 0;
    private double BETTA = 0;
    private double GAMMA = 0;
    private double error = -1;

    private double MIN_SEASONAL = 0.0000001;

    public void predictionCases(Data data, Map<String, Integer> cases, String country) {
        if (cases == null || cases.size() == 0) {
            return;
        }
        EXISTED_PERIOD_AFTER_SEASONAL = Objects.requireNonNull(new File("src/main/resources/data/").list()).length
                - SEASONAL_PERIOD - 2;

        Map<String, List<Integer>> prediction = minimizationOfError(data, cases);

        for (Map.Entry<String, List<Integer>> stringIntegerEntry : prediction.entrySet()) {
            synchronized (data.getPredictionNewCases()) {
                Map<String, List<Integer>> map = data.getPredictionNewCases()
                        .entrySet()
                        .stream()
                        .filter(stringMapEntry ->
                                stringIntegerEntry.getKey().equals(stringMapEntry.getKey()))
                        .findFirst()
                        .map(Map.Entry::getValue)
                        .orElse(new HashMap<>());

                map.put(country, stringIntegerEntry.getValue());
                data.getPredictionNewCases().put(stringIntegerEntry.getKey(), map);
            }
        }
    }

    public void predictionDeaths(Data data, Map<String, Integer> deaths, String country) {
        if (deaths == null || deaths.size() == 0) {
            return;
        }
        Map<String, List<Integer>> prediction = minimizationOfError(data, deaths);

        for (Map.Entry<String, List<Integer>> stringIntegerEntry : prediction.entrySet()) {
            synchronized (data.getPredictionNewDeaths()) {
                Map<String, List<Integer>> map = data.getPredictionNewDeaths()
                        .entrySet()
                        .stream()
                        .filter(stringMapEntry ->
                                stringIntegerEntry.getKey().equals(stringMapEntry.getKey()))
                        .findFirst()
                        .map(Map.Entry::getValue)
                        .orElse(new HashMap<>());
                map.put(country, stringIntegerEntry.getValue());
                data.getPredictionNewDeaths().put(stringIntegerEntry.getKey(), map);
            }
        }
    }

    /**
     * optimization to find the minimum error by changing alpha, betta, gamma constants
     */
    private Map<String, List<Integer>> minimizationOfError(Data data, Map<String, Integer> cases) {
        TreeMap<String, Integer> sortedCasesByDate = sortCasesByDate(data, cases);
        List<Double> seasonal = calculateInitialSeasonal(data, sortedCasesByDate);
        List<Double> seasonalCopy = new ArrayList<>(seasonal);

        List<Double> constants = new ArrayList<>();
        constants.add(0.0);
        constants.add(0.0);
        constants.add(0.0);

        double localError, chunk = 0.1;

        for (int i = 0; i <= 10; i++) {
            for (int j = 0; j <= 10; j++) {
                for (int k = 0; k <= 10; k++) {
                    if (error < 0) {
                        error = predictionForCountryError(data, cases, seasonalCopy, constants);
                        continue;
                    }
                    localError = predictionForCountryError(data, cases, seasonalCopy, constants);
                    if (localError < error) {
                        error = localError;
                        ALPHA = constants.get(0);
                        BETTA = constants.get(1);
                        GAMMA = constants.get(2);
                    }
                    constants.set(0, constants.get(0) + chunk);
                }
                constants.set(0, 0.0);
                constants.set(1, constants.get(1) + chunk);
            }
            constants.set(0, 0.0);
            constants.set(1, 0.0);
            constants.set(2, constants.get(2) + chunk);
        }

        return predictionForCountry(data, cases, seasonal);
    }

    /**
     * @param data     contains all data about calculation and initial information
     * @param cases    of a certain country (new cases or deaths)
     * @param seasonal initial season coefficients
     * @return data about future predictions calculated through exponential smoothing
     */
    public Map<String, List<Integer>> predictionForCountry(Data data, Map<String, Integer> cases, List<Double> seasonal) {
        List<Double> seasonalCopy = new ArrayList<>(seasonal);
        List<Double> level = new ArrayList<>();
        List<Double> trend = new ArrayList<>();
        double alpha = ALPHA, betta = BETTA, gamma = GAMMA;

        if (alpha > 0.99) {
            alpha = 0.99;
        }
        if (betta > 0.99) {
            betta = 0.99;
        }
        if (gamma > 0.99) {
            gamma = 0.99;
        }

        // initial level
        level.add(Precision.round(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD))
                / seasonalCopy.get(0), 5));

        // initial trend
        trend.add(Precision.round(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0)
                - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD - 1)) / seasonalCopy.get(SEASONAL_PERIOD - 1), 5));

        Map<Integer, String> numberLabels = data.getLabelsByNumber();

        for (int i = 0; i < EXISTED_PERIOD_AFTER_SEASONAL - 1; i++) {
            if (level.get(i) == 0) {
                if (new BigDecimal(gamma).equals(new BigDecimal(1))) {
                    seasonalCopy.add(Precision.round(((1 - gamma) * seasonalCopy.get(i)), 7));
                } else {
                    seasonalCopy.add(Precision.round((0.01 * seasonalCopy.get(i)), 7));
                }
            } else {
                seasonalCopy.add(Precision.round((gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + i))
                        / level.get(i) + (1 - gamma) * seasonalCopy.get(i)), 7));
            }
            if (Math.abs(seasonalCopy.get(seasonalCopy.size() - 1)) < MIN_SEASONAL) {
                if (seasonalCopy.get(seasonalCopy.size() - 1) < 0) {
                    seasonalCopy.set(seasonalCopy.size() - 1, -MIN_SEASONAL);
                } else {
                    seasonalCopy.set(seasonalCopy.size() - 1, MIN_SEASONAL);
                }
            }
            level.add(Precision.round((alpha * cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1)) / seasonalCopy.get(i + 1)
                    + (1 - alpha) * (level.get(i) + trend.get(i))), 7));
            trend.add(Precision.round((betta * (level.get(i + 1) - level.get(i)) + (1 - betta) * trend.get(i)), 7));
        }

        seasonalCopy.add(Precision.round((gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + EXISTED_PERIOD_AFTER_SEASONAL - 1))
                / level.get(EXISTED_PERIOD_AFTER_SEASONAL - 1)
                + (1 - gamma) * seasonalCopy.get(EXISTED_PERIOD_AFTER_SEASONAL - 1)), 7));

        Map<String, List<Integer>> predictionsFuture = new HashMap<>();

        String day = LocalDate.now().format(formatter);
        for (int i = 1; i <= SEASONAL_PERIOD * SEASONAL_REPETITION; i++) {
            List<Integer> predictionRange = new ArrayList<>(3);
            predictionRange.add(Math.max((int) Precision.round(((level.get(level.size() - 1)
                    + trend.get(trend.size() - 1))
                    * seasonalCopy.get(seasonalCopy.size() - (SEASONAL_PERIOD * SEASONAL_REPETITION - i) - 1)), 1), 0));
            predictionRange.add(lowBound(predictionRange.get(0)));
            predictionRange.add(highBound(predictionRange.get(0)));
            predictionsFuture.put(day + ".csv", predictionRange);

            day = LocalDate.parse(day, formatter).plusDays(1L).format(formatter);
        }

        return predictionsFuture;
    }

    private Integer lowBound(Integer predictedCases) {
        return Math.max(predictedCases - (int) (error + error / 1.96), 0);
    }

    private Integer highBound(Integer predictedCases) {
        return predictedCases + (int) (error + error / 1.96);
    }

    public double predictionForCountryError(Data data, Map<String, Integer> cases, List<Double> seasonal, List<Double> constants) {
        List<Double> seasonalCopy = new ArrayList<>(seasonal);
        List<Double> level = new ArrayList<>();
        List<Double> trend = new ArrayList<>();
        double alpha = constants.get(0), betta = constants.get(1), gamma = constants.get(2);

        if (alpha > 0.99) {
            alpha = 0.99;
        }
        if (betta > 0.99) {
            betta = 0.99;
        }
        if (gamma > 0.99) {
            gamma = 0.99;
        }

        // initial level
        level.add(Precision.round(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD))
                / seasonalCopy.get(0), 5));

        // initial trend
        trend.add(Precision.round(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0)
                - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD - 1)) / seasonalCopy.get(SEASONAL_PERIOD - 1), 5));

        Map<Integer, String> numberLabels = data.getLabelsByNumber();

        Map<String, Integer> predictionsPast = new TreeMap<>(data.dateComparator());

        for (int i = 0; i < EXISTED_PERIOD_AFTER_SEASONAL - 1; i++) {
            if (level.get(i) == 0) {
                if (new BigDecimal(gamma).equals(new BigDecimal(1))) {
                    seasonalCopy.add(Precision.round(((1 - gamma) * seasonalCopy.get(i)), 7));
                } else {
                    seasonalCopy.add(Precision.round((0.01 * seasonalCopy.get(i)), 7));
                }
                seasonalCopy.add(Precision.round(((1 - gamma) * seasonalCopy.get(i)), 7));
            } else {
                seasonalCopy.add(Precision.round((gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + i))
                        / level.get(i) + (1 - gamma) * seasonalCopy.get(i)), 7));
            }
            if (Math.abs(seasonalCopy.get(seasonalCopy.size() - 1)) < MIN_SEASONAL) {
                if (seasonalCopy.get(seasonalCopy.size() - 1) < 0) {
                    seasonalCopy.set(seasonalCopy.size() - 1, -MIN_SEASONAL);
                } else {
                    seasonalCopy.set(seasonalCopy.size() - 1, MIN_SEASONAL);
                }
            }
            level.add(Precision.round((alpha * cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1)) / seasonalCopy.get(i + 1)
                    + (1 - alpha) * (level.get(i) + trend.get(i))), 7));

            trend.add(Precision.round((betta * (level.get(i + 1) - level.get(i)) + (1 - betta) * trend.get(i)), 7));
            predictionsPast.put(numberLabels.get(SEASONAL_PERIOD + i + 1),
                    (int) Precision.round(((level.get(i) + trend.get(i)) * seasonalCopy.get(i + 1)), 5));
        }

        return RMSE(data, cases, predictionsPast);
    }

    private List<Double> calculateInitialSeasonal(Data data, TreeMap<String, Integer> cases) {
        List<Integer> seasonalPeriodCases = cases.entrySet()
                .stream()
                .filter(stringIntegerEntry -> data.getLabelsByDate().get(stringIntegerEntry.getKey()) < SEASONAL_PERIOD)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        List<Double> list = new ArrayList<>();
        double num = Precision.round(seasonalPeriodCases.stream().mapToDouble(v -> v).sum()
                / seasonalPeriodCases.size(), 7);
        for (Integer seasonalPeriodCase : seasonalPeriodCases) {
            if (num != 0) {
                if (seasonalPeriodCase == 0) {
                    list.add(0.1);
                    continue;
                }
                list.add(Precision.round(seasonalPeriodCase / num, 7));
            } else {
                list.add(1.0);
            }
        }

        return list;
    }

    private TreeMap<String, Integer> sortCasesByDate(Data data, Map<String, Integer> cases) {
        TreeMap<String, Integer> sortedCasesByDate = new TreeMap<>(data.dateComparator());
        sortedCasesByDate.putAll(cases);

        return sortedCasesByDate;
    }

    private double RMSE(Data data, Map<String, Integer> cases, Map<String, Integer> predictions) {

        Map<String, Integer> sortedMap = new TreeMap<>(data.dateComparator());
        Map<String, Integer> withoutSeasonalPeriod = new TreeMap<>(data.dateComparator());
        sortedMap.putAll(cases);
        int counter = 0;
        for (Map.Entry<String, Integer> stringIntegerEntry : sortedMap.entrySet()) {
            if (counter - 1 < SEASONAL_PERIOD) {
                counter++;
                continue;
            }
            withoutSeasonalPeriod.put(stringIntegerEntry.getKey(), stringIntegerEntry.getValue());
        }

        return Precision.round(Math.sqrt(withoutSeasonalPeriod.entrySet()
                .stream()
                .mapToDouble(stringIntegerEntry -> Math.pow((stringIntegerEntry.getValue() - predictions
                        .entrySet()
                        .stream()
                        .filter(integerEntry -> integerEntry.getKey().equals(stringIntegerEntry.getKey()))
                        .findFirst()
                        .map(Map.Entry::getValue)
                        .orElse(0)), 2))
                .sum() / withoutSeasonalPeriod.size()), 3);
    }

    private double MASE(Data data, Map<String, Integer> cases, Map<String, Integer> predictions) {
        double mae = MAE(data, cases, predictions);

        Map<String, Integer> sortedMap = new TreeMap<>(data.dateComparator());
        sortedMap.putAll(cases);

        int lastValue = -1;
        double sum = 0.0;

        for (Map.Entry<String, Integer> stringIntegerEntry : sortedMap.entrySet()) {
            if (lastValue == -1) {
                lastValue = stringIntegerEntry.getValue();
                continue;
            }
            sum += Math.abs(stringIntegerEntry.getValue() - lastValue);
            lastValue = stringIntegerEntry.getValue();
        }

        return mae / sum;
    }

    private double MAE(Data data, Map<String, Integer> cases, Map<String, Integer> predictions) {
        Map<String, Integer> sortedMap = new TreeMap<>(data.dateComparator());
        Map<String, Integer> withoutSeasonalPeriod = new TreeMap<>(data.dateComparator());
        sortedMap.putAll(cases);
        int counter = 0;
        for (Map.Entry<String, Integer> stringIntegerEntry : sortedMap.entrySet()) {
            if (counter - 1 < SEASONAL_PERIOD) {
                counter++;
                continue;
            }
            withoutSeasonalPeriod.put(stringIntegerEntry.getKey(), stringIntegerEntry.getValue());
        }

        double sum = withoutSeasonalPeriod.entrySet().stream().mapToDouble(stringIntegerEntry -> Math.abs((stringIntegerEntry.getValue() - predictions.entrySet()
                        .stream()
                        .filter(integerEntry -> integerEntry.getKey().equals(stringIntegerEntry.getKey()))
                        .findFirst()
                        .map(Map.Entry::getValue)
                        .orElse(0))))
                .sum();

        return sum / predictions.size();
    }
}
