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
    private final int EXISTED_PERIOD_AFTER_SEASONAL = 83;

    private double ALPHA = 0;
    private double BETTA = 0;
    private double GAMMA = 0;
    private double error = -1;

    public void prediction(Data data, Map<String, Integer> cases) {
        minimizationOfError(data, cases);
    }

    private void minimizationOfError(Data data, Map<String, Integer> cases) {
        TreeMap<String, Integer> sortedCasesByDate = sortCasesByDate(data, cases);
        List<Double> seasonal = calculateInitialSeasonal(data, sortedCasesByDate);
        List<Double> seasonalCopy = new ArrayList<>(seasonal);

        List<Double> constants = new ArrayList<>();
        constants.add(0.0);
        constants.add(0.0);
        constants.add(0.0);

        double localError, chunk = 0.001;

        Date start = new Date();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                for (int k = 0; k < 10; k++) {
                    if(error < 0) {
                        error = predictionForCountryError(data, cases, seasonalCopy, constants);
                        continue;
                    }
                    localError = predictionForCountryError(data, cases, seasonalCopy, constants);
                    if(localError < error) {
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

//        List<Double> constants = binarySearchGamma(deliminator, data, cases, seasonalCopy);
//        constants.set(2, 0.154255);
//        constants.set(1, 0.1);
//        constants.set(0, 0.950788);
        constants.set(0, ALPHA);
        constants.set(1, BETTA);
        constants.set(2, GAMMA);
//        log.warn("Alpha: " + constants.get(2));
//        log.warn("Betta: " + constants.get(1));
//        log.warn("Gamma: " + constants.get(0));
        log.warn("Alpha: " + ALPHA);
        log.warn("Betta: " + BETTA);
        log.warn("Gamma: " + GAMMA);
        log.warn("Error: " + error);
        log.warn("Seconds: " + seconds);

//        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
//            log.warn(stringIntegerEntry.getKey() + " : " + stringIntegerEntry.getValue());
//        }

        for (Map.Entry<String, Integer> stringIntegerEntry : sortedCasesByDate.entrySet()) {
            log.warn(stringIntegerEntry.getKey() + " : " + stringIntegerEntry.getValue());
        }

        for (Map.Entry<String, Integer> stringIntegerEntry : predictionForCountry(data, cases, seasonal).entrySet()) {
            log.warn("Prediction: " + stringIntegerEntry.getKey() + " : " + stringIntegerEntry.getValue());
        }

//        data.getPredictionNewCases().put("World", result);
    }

    public Map<String, Integer> predictionForCountry(Data data, Map<String, Integer> cases, List<Double> seasonal) {
        List<Double> seasonalCopy = new ArrayList<>(seasonal);
        List<Double> level = new ArrayList<>();
        List<Double> trend = new ArrayList<>();
//        double alpha = constants.get(0), betta = constants.get(1), gamma = constants.get(2);
        double alpha = 0.940976769931106, betta = 0.124793023, gamma = 0.122765775;

        // initial level
        level.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD + 1)) / seasonalCopy.get(0));
//        double lastLevel = cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0);
        // initial trend
        trend.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD + 1)) / seasonalCopy.get(0)
                - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(SEASONAL_PERIOD - 1));
//        double lastTrend = cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0)
//                - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD - 1)) / seasonalCopy.get(SEASONAL_PERIOD - 1);

        Map<Integer, String> numberLabels = data.getLabelsByNumber();

        Map<String, Integer> predictionsPast = new TreeMap<>(data.dateComparator());

        for (int i = 0; i < EXISTED_PERIOD_AFTER_SEASONAL - 1; i++) {
            seasonalCopy.add((gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1))
                    / level.get(i) + (1 - gamma) * seasonalCopy.get(i)));
            level.add((alpha / (cases.get(numberLabels.get(SEASONAL_PERIOD + i + 2)) / seasonalCopy.get(i + 1))
                    + (1 - alpha) * (level.get(i) + trend.get(i))));
            trend.add((betta * (level.get(i + 1) - level.get(i)) + (1 - betta) * trend.get(i)));
            predictionsPast.put(numberLabels.get(SEASONAL_PERIOD + i + 2),
                    (int) ((level.get(i + 1) + trend.get(i + 1)) * seasonalCopy.get(i + 1)));
        }

        seasonalCopy.add((gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + EXISTED_PERIOD_AFTER_SEASONAL))
                / level.get(EXISTED_PERIOD_AFTER_SEASONAL - 1)
                + (1 - gamma) * seasonalCopy.get(EXISTED_PERIOD_AFTER_SEASONAL - 1)));

        Map<String, Integer> predictionsFuture = new TreeMap<>(data.dateComparator());

        String day = LocalDate.now().format(formatter);
        for (int i = 1; i <= SEASONAL_PERIOD; i++) { // multiply i
            predictionsFuture.put(day + ".csv", (int) ((level.get(level.size() - 1) + trend.get(trend.size() - 1))
                    * seasonalCopy.get(seasonalCopy.size() - (SEASONAL_PERIOD - i) - 1)));

            day = LocalDate.parse(day, formatter).plusDays(1L).format(formatter);
        }

        log.warn("" + RMSE(data, cases, predictionsPast));

        return predictionsFuture;

//        data.getPredictionNewCases().put("World", predictionsFuture);
    }

    public double predictionForCountryError(Data data, Map<String, Integer> cases, List<Double> seasonal, List<Double> constants) {
        List<Double> seasonalCopy = new ArrayList<>(seasonal);
        List<Double> level = new ArrayList<>();
        List<Double> trend = new ArrayList<>();
        double alpha = constants.get(0), betta = constants.get(1), gamma = constants.get(2);

        // initial level
        level.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD + 1)) / seasonalCopy.get(0));
//        double lastLevel = cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0);
        // initial trend
        trend.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD + 1)) / seasonalCopy.get(0)
                - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(SEASONAL_PERIOD - 1));
//        double lastTrend = cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonalCopy.get(0)
//                - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD - 1)) / seasonalCopy.get(SEASONAL_PERIOD - 1);

        Map<Integer, String> numberLabels = data.getLabelsByNumber();

        Map<String, Integer> predictionsPast = new TreeMap<>(data.dateComparator());

        for (int i = 0; i < EXISTED_PERIOD_AFTER_SEASONAL - 1; i++) {
            seasonalCopy.add((gamma * cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1))
                    / level.get(i) + (1 - gamma) * seasonalCopy.get(i)));
//            log.warn(""+cases.get(numberLabels.get(SEASONAL_PERIOD + i + 2)));
//            log.warn(""+seasonalCopy.get(i+1));
            level.add((alpha / (cases.get(numberLabels.get(SEASONAL_PERIOD + i + 2)) / seasonalCopy.get(i + 1))
                    + (1 - alpha) * (level.get(i) + trend.get(i))));
            trend.add((betta * (level.get(i + 1) - level.get(i)) + (1 - betta) * trend.get(i)));
            predictionsPast.put(numberLabels.get(SEASONAL_PERIOD + i + 2),
                    (int) ((level.get(i + 1) + trend.get(i + 1)) * seasonalCopy.get(i + 1)));
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
            if (counter - 1 < SEASONAL_PERIOD * 2) {
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
