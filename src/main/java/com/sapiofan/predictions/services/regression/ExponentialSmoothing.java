package com.sapiofan.predictions.services.regression;

import com.sapiofan.predictions.entities.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ExponentialSmoothing {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private static final Logger log = LoggerFactory.getLogger(ExponentialSmoothing.class);

    private final int SEASONAL_PERIOD = 7;
    private final int EXISTED_PERIOD_AFTER_SEASONAL = 113;
    private final int SEASONAL_REPETITION = 3;

    private double ALPHA = 0;
    private double BETTA = 0;
    private double GAMMA = 0;
    private double error = -1;

    public void predictionCases(Data data, Map<String, Integer> cases, String country) {
        if (cases == null || cases.size() == 0) {
            return;
        }
        Map<String, Integer> prediction = minimizationOfError(data, cases);
        for (Map.Entry<String, Integer> stringIntegerEntry : prediction.entrySet()) {
            Map<String, Integer> map;
            synchronized (data) {
                map = data.getPredictionNewCases()
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
        Map<String, Integer> prediction = minimizationOfError(data, deaths);
        for (Map.Entry<String, Integer> stringIntegerEntry : prediction.entrySet()) {
            synchronized (data) {
                Map<String, Integer> map = data.getPredictionNewDeaths()
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

    private Map<String, Integer> minimizationOfError(Data data, Map<String, Integer> cases) {
        TreeMap<String, Integer> sortedCasesByDate = sortCasesByDate(data, cases);
        List<Double> seasonal = calculateInitialSeasonal(data, sortedCasesByDate);
        List<Double> seasonalCopy = new ArrayList<>(seasonal);

        List<Double> constants = new ArrayList<>();
        constants.add(0.0);
        constants.add(0.0);
        constants.add(0.0);

        double localError, chunk = 0.1;

        Date start = new Date();
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
        Date end = new Date();
        long seconds = (end.getTime() - start.getTime()) / 1000;

        log.warn("Alpha: " + ALPHA);
        log.warn("Betta: " + BETTA);
        log.warn("Gamma: " + GAMMA);
        log.warn("Error: " + error);
        log.warn("Seconds: " + seconds);

//        for (Map.Entry<String, Integer> stringIntegerEntry : sortedCasesByDate.entrySet()) {
//            log.warn(stringIntegerEntry.getKey() + " : " + stringIntegerEntry.getValue());
//        }
//
//        for (Map.Entry<String, Integer> stringIntegerEntry : predictionForCountry(data, cases, seasonal).entrySet()) {
//            log.warn("Prediction: " + stringIntegerEntry.getKey() + " : " + stringIntegerEntry.getValue());
//        }

        return predictionForCountry(data, cases, seasonal);
    }

    public Map<String, Integer> predictionForCountry(Data data, Map<String, Integer> cases, List<Double> seasonal) {
        List<Double> seasonalCopy = new ArrayList<>(seasonal);
        List<Double> level = new ArrayList<>();
        List<Double> trend = new ArrayList<>();
        double alpha = ALPHA, betta = BETTA, gamma = GAMMA;

        // initial level
        level.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD + 1)) / seasonalCopy.get(0));

        // initial trend
        trend.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD + 1)) / seasonalCopy.get(0)
                - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(SEASONAL_PERIOD - 1));

        Map<Integer, String> numberLabels = data.getLabelsByNumber();

        Map<String, Integer> predictionsPast = new TreeMap<>(data.dateComparator());

        for (int i = 0; i < EXISTED_PERIOD_AFTER_SEASONAL - 1; i++) {
            seasonalCopy.add((gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1))
                    / level.get(i) + (1 - gamma) * seasonalCopy.get(i)));
            level.add((alpha * (cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1)) / seasonalCopy.get(i + 1))
                    + (1 - alpha) * (level.get(i) + trend.get(i))));
            trend.add((betta * (level.get(i + 1) - level.get(i)) + (1 - betta) * trend.get(i)));
            predictionsPast.put(numberLabels.get(SEASONAL_PERIOD + i + 1),
                    (int) ((level.get(i + 1) + trend.get(i + 1)) * seasonalCopy.get(i + 1)));
        }

        seasonalCopy.add((gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + EXISTED_PERIOD_AFTER_SEASONAL - 1))
                / level.get(EXISTED_PERIOD_AFTER_SEASONAL - 1)
                + (1 - gamma) * seasonalCopy.get(EXISTED_PERIOD_AFTER_SEASONAL - 1)));

        Map<String, Integer> predictionsFuture = new HashMap<>();

        String day = LocalDate.now().format(formatter);
        for (int i = 1; i <= SEASONAL_PERIOD * SEASONAL_REPETITION; i++) {
            predictionsFuture.put(day + ".csv", Math.max((int) ((level.get(level.size() - 1) + trend.get(trend.size() - 1))
                    * seasonalCopy.get(seasonalCopy.size() - (SEASONAL_PERIOD * SEASONAL_REPETITION - i) - 1)), 0));

            day = LocalDate.parse(day, formatter).plusDays(1L).format(formatter);
        }

        return predictionsFuture;
    }

    public double predictionForCountryError(Data data, Map<String, Integer> cases, List<Double> seasonal, List<Double> constants) {
        List<Double> seasonalCopy = new ArrayList<>(seasonal);
        List<Double> level = new ArrayList<>();
        List<Double> trend = new ArrayList<>();
        double alpha = constants.get(0), betta = constants.get(1), gamma = constants.get(2);

        // initial level
//        log.warn(""+data.getLabelsByNumber().size());
//        log.warn(""+data.getLabelsByNumber().get(SEASONAL_PERIOD + 1));
        try {
            level.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD + 1)) / seasonalCopy.get(0));
        } catch (NullPointerException e) {
            log.warn(""+data.getLabelsByNumber().get(SEASONAL_PERIOD + 1));
            log.warn(""+cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD + 1)));
            System.exit(0);
        }

        // initial trend
        try {
            trend.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD + 1)) / seasonalCopy.get(0)
                    - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(SEASONAL_PERIOD - 1));
        } catch (NullPointerException e) {
            log.warn(""+data.getLabelsByNumber().get(SEASONAL_PERIOD));
            log.warn(""+data.getLabelsByNumber().get(SEASONAL_PERIOD + 1));
            log.warn(""+cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD + 1)));
            log.warn(""+seasonalCopy.get(SEASONAL_PERIOD - 1));
            System.exit(0);
        }

        Map<Integer, String> numberLabels = data.getLabelsByNumber();

        Map<String, Integer> predictionsPast = new TreeMap<>(data.dateComparator());

        for (int i = 0; i < EXISTED_PERIOD_AFTER_SEASONAL - 1; i++) {
            seasonalCopy.add((gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + i))
                    / level.get(i) + (1 - gamma) * seasonalCopy.get(i)));
            level.add((alpha * (cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1)) / seasonalCopy.get(i + 1))
                    + (1 - alpha) * (level.get(i) + trend.get(i))));
            trend.add((betta * (level.get(i + 1) - level.get(i)) + (1 - betta) * trend.get(i)));
            predictionsPast.put(numberLabels.get(SEASONAL_PERIOD + i + 1),
                    (int) ((level.get(i + 1) + trend.get(i + 1)) * seasonalCopy.get(i + 1)));
        }

        return RMSE(data, cases, predictionsPast);
    }

    private List<Double> calculateInitialSeasonal(Data data, TreeMap<String, Integer> cases) {
        List<Double> seasonal = new ArrayList<>();
        List<Integer> firstMonthCases = new ArrayList<>();

        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
            if (data.getLabelsByDate().get(stringIntegerEntry.getKey()) <= SEASONAL_PERIOD) {
                firstMonthCases.add(stringIntegerEntry.getValue());
            }
        }

        double average = firstMonthCases.stream().mapToDouble(v -> v).sum() / firstMonthCases.size();

        for (Integer firstMonthCase : firstMonthCases) {
            seasonal.add(firstMonthCase / average);
        }

        return seasonal;
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

        double sum = 0.0;
        for (Map.Entry<String, Integer> stringIntegerEntry : withoutSeasonalPeriod.entrySet()) {
            int predictedCase = 0;
            for (Map.Entry<String, Integer> integerEntry : predictions.entrySet()) {
                if (integerEntry.getKey().equals(stringIntegerEntry.getKey())) {
                    predictedCase = integerEntry.getValue();
                    break;
                }
            }
            double pow = Math.pow((stringIntegerEntry.getValue() - predictedCase), 2);
            sum += pow;
        }

        return Math.sqrt(sum / withoutSeasonalPeriod.size());
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

        double sum = 0.0;
        for (Map.Entry<String, Integer> stringIntegerEntry : withoutSeasonalPeriod.entrySet()) {
            int predictedCase = 0;
            for (Map.Entry<String, Integer> integerEntry : predictions.entrySet()) {
                if (integerEntry.getKey().equals(stringIntegerEntry.getKey())) {
                    predictedCase = integerEntry.getValue();
                    break;
                }
            }
            double pow = Math.abs((stringIntegerEntry.getValue() - predictedCase));
            sum += pow;
        }

        return sum / predictions.size();
    }
}
