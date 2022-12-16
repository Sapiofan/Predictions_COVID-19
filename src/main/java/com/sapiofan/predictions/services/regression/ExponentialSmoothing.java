package com.sapiofan.predictions.services.regression;

import com.sapiofan.predictions.entities.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ExponentialSmoothing {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final int SEASONAL_PERIOD = 30;
    private final int EXISTED_PERIOD_AFTER_SEASONAL = 60;
    // decrease rmse that is assign new values for alpha, betta, gamma
    private double ALPHA = 0.1;
    private double BETTA = 0.1;
    private double GAMMA = 0.1;

    public void prediction(Data data) {

    }

    public void predictionForCountry(Data data, Map<String, Integer> cases) {
        TreeMap<String, Integer> sortedCasesByDate = sortCasesByDate(data, cases);
        List<Double> seasonal = calculateInitialSeasonal(data, sortedCasesByDate);
        List<Double> level = new ArrayList<>();
        List<Double> trend = new ArrayList<>();

        // initial level
        level.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonal.get(0));

        // initial trend
        trend.add(cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD)) / seasonal.get(0)
                - cases.get(data.getLabelsByNumber().get(SEASONAL_PERIOD - 1)) / seasonal.get(SEASONAL_PERIOD - 1));

        Map<Integer, String> numberLabels = data.getLabelsByNumber();

        Map<String, Double> predictionsPast = new TreeMap<>(data.dateComparator());

        for (int i = 0; i < EXISTED_PERIOD_AFTER_SEASONAL; i++) {
            seasonal.add(GAMMA * cases.get(numberLabels.get(SEASONAL_PERIOD + i)) / level.get(i) + (1 - GAMMA) * seasonal.get(i));
            level.add(ALPHA / (cases.get(numberLabels.get(SEASONAL_PERIOD + i + 1)) / seasonal.get(i + 1))
                    + (1 - BETTA) * (level.get(i) + trend.get(i)));
            trend.add(BETTA * (level.get(i + 1) - level.get(i)) + (1 - BETTA) * trend.get(i));
            predictionsPast.put(numberLabels.get(SEASONAL_PERIOD), (level.get(i) + trend.get(i)) * seasonal.get(i + 1));
        }

        Map<String, Double> predictionsFuture = new TreeMap<>(data.dateComparator());

        String day = LocalDate.now().format(formatter);
        for (int i = 1; i <= SEASONAL_PERIOD; i++) {
            predictionsFuture.put(day + ".csv", (level.get(level.size() - 1) + i * trend.get(trend.size() - 1))
                    * seasonal.get(seasonal.size() - (SEASONAL_PERIOD - i + 1)));
            day = LocalDate.parse(day).plusDays(1L).format(formatter);
        }
    }

    private List<Double> calculateInitialSeasonal(Data data, TreeMap<String, Integer> cases) {
        List<Double> seasonal = new ArrayList<>();
        List<Integer> firstMonthCases = new ArrayList<>();

        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
            if (data.getLabelsByDate().get(stringIntegerEntry.getKey()) < SEASONAL_PERIOD) {
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
        return Math.sqrt(cases.entrySet()
                .stream()
                .mapToDouble(stringIntegerEntry ->
                        Math.pow(stringIntegerEntry.getValue() - predictions.get(stringIntegerEntry.getKey()), 2))
                .sum() / cases.size());
    }
}
