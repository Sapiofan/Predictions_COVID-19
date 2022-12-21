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

    private final int SEASONAL_PERIOD = 30;
    private final int EXISTED_PERIOD_AFTER_SEASONAL = 60;
    // decrease rmse that is assign new values for alpha, betta, gamma
    private double ALPHA = 0;
    private double BETTA = 0;
    private double GAMMA = 0;
    private double error = -1;

    public void prediction(Data data, Map<String, Integer> cases) {
        minimizationOfError(data, cases);
    }

    private void minimizationOfError(Data data, Map<String, Integer> cases) {
        int deliminator = 1000;
        TreeMap<String, Integer> sortedCasesByDate = sortCasesByDate(data, cases);
        List<Double> seasonal = calculateInitialSeasonal(data, sortedCasesByDate);
        List<Double> seasonalCopy = new ArrayList<>(seasonal);
        List<Double> constants = binarySearchGamma(deliminator, data, cases, seasonalCopy);
        log.warn("Alpha: " + constants.get(2));
        log.warn("Betta: " + constants.get(1));
        log.warn("Gamma: " + constants.get(0));
        log.warn("Error: " + error);
        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
            log.warn(stringIntegerEntry.getKey() + " : " + stringIntegerEntry.getValue());
        }

        for (Map.Entry<String, Integer> stringIntegerEntry : predictionForCountry(data, cases, seasonal, constants).entrySet()) {
            log.warn("Prediction: " + stringIntegerEntry.getKey() + " : " + stringIntegerEntry.getValue());
        }

//        data.getPredictionNewCases().put("World", result);
    }

    private List<Double> binarySearchGamma(int deliminator, Data data, Map<String, Integer> cases, List<Double> seasonal) {
        int l = 0, r = deliminator;
        double localError;
        boolean flag = false;
        int difference;
        List<Double> constants = new ArrayList<>();
        constants.add(-1.0);
        constants.add(-1.0);
        constants.add(-1.0);
        while (l <= r) {
            difference = (r - l) / 2;
            int m = l + difference;

            constants.set(0, (double) m / (double) deliminator);
            binarySearchBetta(deliminator, data, cases, seasonal, constants);

            localError = predictionForCountryError(data, cases, seasonal, constants);

            // If x greater, ignore left half
            if (error > localError) {
                l = m + 1;
            } else {
                r = m - 1;
//                r = l - difference;
            }
        }

        return constants;
    }

    private void binarySearchBetta(int deliminator, Data data, Map<String, Integer> cases, List<Double> seasonal,
                                           List<Double> constants) {
        int l = 0, r = deliminator;
        double localError;
        boolean flag = false;
        int difference;
        while (l <= r) {
            difference = (r - l) / 2;
            int m = l + difference;

            constants.set(1, (double) m / (double) deliminator);
            binarySearchAlpha(deliminator, data, cases, seasonal, constants);

            localError = predictionForCountryError(data, cases, seasonal, constants);

            // If x greater, ignore left half
            if (error > localError) {
                l = m + 1;
            } else {
                r = m - 1;
//                r = l - difference;
            }
        }
    }

    private void binarySearchAlpha(int deliminator, Data data, Map<String, Integer> cases, List<Double> seasonal,
                                   List<Double> constants) {
        int l = 0, r = deliminator;
        double localError;
        boolean flag = false;
        int difference;
        while (l <= r) {
            difference = (r - l) / 2;
            int m = l + difference;

            constants.set(2, (double) m / (double) deliminator);
            localError = predictionForCountryError(data, cases, seasonal, constants);

            if (error == -1) {
                error = localError;
                l = m + 1;
                flag = true;
                continue;
            }

//            if(flag && error < localError) {
//                l = 0;
//                flag = false;
//                r = l + (r - l) / 2 - 1;
//                continue;
//            }

            if (error > localError) {
                l = m + 1;
            } else {
                r = m - 1;
//                r = l - difference;
            }
        }
    }

    public Map<String, Integer> predictionForCountry(Data data, Map<String, Integer> cases, List<Double> seasonal, List<Double> constants) {
        List<Double> seasonalCopy = new ArrayList<>(seasonal);
        List<Double> level = new ArrayList<>();
        List<Double> trend = new ArrayList<>();
        double alpha = constants.get(0), betta = constants.get(1), gamma = constants.get(2);

        // initial level
        level.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0));
//        double lastLevel = cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0);
        // initial trend
        trend.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0)
                - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD - 1)) / seasonalCopy.get(SEASONAL_PERIOD - 1));
//        double lastTrend = cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0)
//                - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD - 1)) / seasonalCopy.get(SEASONAL_PERIOD - 1);

        Map<Integer, String> numberLabels = data.getLabelsByNumber();

        Map<String, Integer> predictionsPast = new TreeMap<>(data.dateComparator());

//        double newLevel, newTrend;

        for (int i = 0; i < EXISTED_PERIOD_AFTER_SEASONAL - 1; i++) {
//            log.warn(""+(SEASONAL_PERIOD + i + 1));
//            log.warn(""+numberLabels.get(SEASONAL_PERIOD + i + 1));
//            log.warn(""+cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1)));
//            log.warn(""+seasonal.get(i + 1));
            seasonalCopy.add(gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + i))
                    / level.get(i) + (1 - gamma) * seasonalCopy.get(i));
            level.add(alpha / (cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1)) / seasonalCopy.get(i + 1))
                    + (1 - betta) * (level.get(i) + trend.get(i)));
            trend.add(betta * (level.get(i + 1) - level.get(i)) + (1 - betta) * trend.get(i));
            predictionsPast.put(numberLabels.get(SEASONAL_PERIOD + i),
                    (int) ((level.get(i) + trend.get(i)) * seasonalCopy.get(i + 1)));
//            seasonalCopy.add(gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + i))
//                    / lastLevel + (1 - gamma) * seasonalCopy.get(i));
//            newLevel = alpha / (cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1)) / seasonalCopy.get(i + 1))
//                    + (1 - betta) * (lastLevel + lastTrend);
//            newTrend = betta * (newLevel - lastLevel) + (1 - betta) * lastTrend;
//            predictionsPast.put(numberLabels.get(SEASONAL_PERIOD + i),
//                    (int) ((lastLevel + lastTrend) * seasonalCopy.get(i + 1)));
//            lastLevel = newLevel;
//            lastTrend = newTrend;
        }

        Map<String, Integer> predictionsFuture = new TreeMap<>(data.dateComparator());

        String day = LocalDate.now().format(formatter);
        for (int i = 1; i <= SEASONAL_PERIOD; i++) {
            predictionsFuture.put(day + ".csv", (int) ((level.get(level.size() - 1) + i * trend.get(trend.size() - 1))
                    * seasonal.get(seasonal.size() - (SEASONAL_PERIOD - i + 1))));
            day = LocalDate.parse(day, formatter).plusDays(1L).format(formatter);
        }

        return predictionsFuture;

//        data.getPredictionNewCases().put("World", predictionsFuture);
    }

    public double predictionForCountryError(Data data, Map<String, Integer> cases, List<Double> seasonal, List<Double> constants) {
        List<Double> seasonalCopy = new ArrayList<>(seasonal);
//        List<Double> level = new ArrayList<>();
//        List<Double> trend = new ArrayList<>();
        double alpha = constants.get(0), betta = constants.get(1), gamma = constants.get(2);

        // initial level
//        level.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0));
        double lastLevel = cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0);
        // initial trend
//        trend.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0)
//                - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD - 1)) / seasonalCopy.get(SEASONAL_PERIOD - 1));
        double lastTrend = cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0)
                - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD - 1)) / seasonalCopy.get(SEASONAL_PERIOD - 1);

        Map<Integer, String> numberLabels = data.getLabelsByNumber();

        Map<String, Integer> predictionsPast = new TreeMap<>(data.dateComparator());

        double newLevel, newTrend;

        for (int i = 0; i < EXISTED_PERIOD_AFTER_SEASONAL - 1; i++) {
//            log.warn(""+(SEASONAL_PERIOD + i + 1));
//            log.warn(""+numberLabels.get(SEASONAL_PERIOD + i + 1));
//            log.warn(""+cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1)));
//            log.warn(""+seasonal.get(i + 1));
//            seasonalCopy.add(gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + i))
//                    / level.get(i) + (1 - gamma) * seasonalCopy.get(i));
//            level.add(alpha / (cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1)) / seasonalCopy.get(i + 1))
//                    + (1 - betta) * (level.get(i) + trend.get(i)));
//            trend.add(betta * (level.get(i + 1) - level.get(i)) + (1 - betta) * trend.get(i));
//            predictionsPast.put(numberLabels.get(SEASONAL_PERIOD + i),
//                    (int) ((level.get(i) + trend.get(i)) * seasonalCopy.get(i + 1)));
            seasonalCopy.add(gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + i))
                    / lastLevel + (1 - gamma) * seasonalCopy.get(i));
            newLevel = alpha / (cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1)) / seasonalCopy.get(i + 1))
                    + (1 - betta) * (lastLevel + lastTrend);
            newTrend = betta * (newLevel - lastLevel) + (1 - betta) * lastTrend;
            predictionsPast.put(numberLabels.get(SEASONAL_PERIOD + i),
                    (int) ((lastLevel + lastTrend) * seasonalCopy.get(i + 1)));
            lastLevel = newLevel;
            lastTrend = newTrend;
        }

        return RMSE(data, cases, predictionsPast);


//        Map<String, Integer> predictionsFuture = new TreeMap<>(data.dateComparator());
//
//        String day = LocalDate.now().format(formatter);
//        for (int i = 1; i <= SEASONAL_PERIOD; i++) {
//            predictionsFuture.put(day + ".csv", (int) ((level.get(level.size() - 1) + i * trend.get(trend.size() - 1))
//                    * seasonal.get(seasonal.size() - (SEASONAL_PERIOD - i + 1))));
//            day = LocalDate.parse(day, formatter).plusDays(1L).format(formatter);
//        }

//        data.getPredictionNewCases().put("World", predictionsFuture);
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
            if (counter < SEASONAL_PERIOD) {
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
}
