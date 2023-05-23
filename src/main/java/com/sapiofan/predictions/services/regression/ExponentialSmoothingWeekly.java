package com.sapiofan.predictions.services.regression;

import com.sapiofan.predictions.entities.Data;
import com.sapiofan.predictions.services.impl.Utils;
import org.apache.commons.math3.util.Precision;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class ExponentialSmoothingWeekly extends ExponentialSmoothing {

    private final int SEASONAL_PERIOD_FOR_WEEK = 3;

    public boolean checkWeekSeasonality(Map<String, Integer> cases) {
        String day = LocalDate.now().minusDays(4 * SEASONAL_PERIOD).format(formatter) + ".csv";
        int nonZeros = 0;
        for (int i = 0; i < 4 * SEASONAL_PERIOD; i++) {
            if (cases.get(day) != null && cases.get(day) != 0) {
                nonZeros++;
            }
            day = LocalDate.parse(day.substring(0, day.indexOf(".")), formatter).plusDays(1).format(formatter) + ".csv";
        }

        return nonZeros > 4;
    }

    public Map<String, List<Integer>> minimizationOfError(Data data, Map<String, Integer> cases) {
        EXISTED_PERIOD_AFTER_SEASONAL = cases.size() - SEASONAL_PERIOD - 2;
        // sort cases by dates
        TreeMap<String, Integer> sortedCasesByDate = sortCasesByDate(data, cases);
        // calculate first 7 initial seasonal coefficients
        List<Double> seasonal = calculateInitialSeasonal(sortedCasesByDate);
        List<Double> seasonalCopy = new ArrayList<>(seasonal);

        // assign initial constants as 0
        List<Double> constants = new ArrayList<>();
        constants.add(0.0);
        constants.add(0.0);
        constants.add(0.0);

        double localError, chunk = 0.1;

        try {
            // iterate through fixed coefficients to find minimum error
            for (int i = 0; i <= 10; i++) {
                for (int j = 0; j <= 10; j++) {
                    for (int k = 0; k <= 10; k++) {
                        if (error < 0) {
                            error = predictionForCountryError(data, sortedCasesByDate, seasonalCopy, constants);
                            continue;
                        }
                        localError = predictionForCountryError(data, sortedCasesByDate, seasonalCopy, constants);
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
        } catch (IndexOutOfBoundsException e) {
            Map<String, List<Integer>> predictionsFuture = new HashMap<>();

            // define last date of existed data
            String day = LocalDate.parse(sortedCasesByDate.lastKey()
                            .substring(0, sortedCasesByDate.lastKey().indexOf(".")), formatter)
                    .plusDays(1).format(formatter);
            for (int i = 1; i <= SEASONAL_PERIOD * SEASONAL_REPETITION; i++) {
                List<Integer> predictionRange = new ArrayList<>(3);
                predictionRange.add(0);
                predictionRange.add(0);
                predictionRange.add(0);
                predictionsFuture.put(day + ".csv", predictionRange);

                day = LocalDate.parse(day, formatter).plusDays(1L).format(formatter);
            }
        }

        constants.set(0, ALPHA);
        constants.set(1, BETTA);
        constants.set(2, GAMMA);
        boundsSmoothing(constants);
        ALPHA = constants.get(0);
        BETTA = constants.get(1);
        GAMMA = constants.get(2);

        return predictionForCountry(sortedCasesByDate, seasonal);
    }

    public double predictionForCountryError(Data data, TreeMap<String, Integer> cases, List<Double> seasonal, List<Double> constants) {
        List<Double> seasonalCopy = new ArrayList<>(seasonal);
        List<Double> level = new ArrayList<>();
        List<Double> trend = new ArrayList<>();
        boundsSmoothing(constants);
        double alpha = constants.get(0), betta = constants.get(1), gamma = constants.get(2);

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
            keys.add(stringIntegerEntry.getKey());
            values.add(stringIntegerEntry.getValue());
        }

        // initial level
        level.add(Precision.round(values.get(SEASONAL_PERIOD_FOR_WEEK)
                / seasonalCopy.get(0), 5));

        // initial trend
        trend.add(Precision.round(values.get(SEASONAL_PERIOD_FOR_WEEK) / seasonalCopy.get(0)
                - values.get(SEASONAL_PERIOD_FOR_WEEK - 1)
                / seasonalCopy.get(SEASONAL_PERIOD_FOR_WEEK - 1), 5));

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
                seasonalCopy.add(Precision.round((gamma * values.get(SEASONAL_PERIOD_FOR_WEEK + i)
                        / level.get(i) + (1 - gamma) * seasonalCopy.get(i)), 7));
            }
            if (Math.abs(seasonalCopy.get(seasonalCopy.size() - 1)) < MIN_SEASONAL) {
                if (seasonalCopy.get(seasonalCopy.size() - 1) < 0) {
                    seasonalCopy.set(seasonalCopy.size() - 1, -MIN_SEASONAL);
                } else {
                    seasonalCopy.set(seasonalCopy.size() - 1, MIN_SEASONAL);
                }
            }
            level.add(Precision.round((alpha * values.get(SEASONAL_PERIOD_FOR_WEEK + i + 1)
                    / seasonalCopy.get(i + 1) + (1 - alpha) * (level.get(i) + trend.get(i))), 7));

            trend.add(Precision.round((betta * (level.get(i + 1) - level.get(i)) + (1 - betta) * trend.get(i)), 7));
            predictionsPast.put(keys.get(SEASONAL_PERIOD_FOR_WEEK + i + 1),
                    (int) Precision.round(((level.get(i) + trend.get(i)) * seasonalCopy.get(i + 1)), 5));
        }

        return RMSE(data, cases, predictionsPast);
    }

    public Map<String, List<Integer>> predictionForCountry(TreeMap<String, Integer> cases, List<Double> seasonal) {
        List<Double> seasonalCopy = new ArrayList<>(seasonal);
        List<Double> level = new ArrayList<>();
        List<Double> trend = new ArrayList<>();
        double alpha = ALPHA, betta = BETTA, gamma = GAMMA;

        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
            keys.add(stringIntegerEntry.getKey());
            values.add(stringIntegerEntry.getValue());
        }

        // initial level
        level.add(Precision.round(values.get(SEASONAL_PERIOD_FOR_WEEK)
                / seasonalCopy.get(0), 5));

        // initial trend
        trend.add(Precision.round(values.get(SEASONAL_PERIOD_FOR_WEEK) / seasonalCopy.get(0)
                - values.get(SEASONAL_PERIOD_FOR_WEEK - 1)
                / seasonalCopy.get(SEASONAL_PERIOD_FOR_WEEK - 1), 5));

        for (int i = 0; i < EXISTED_PERIOD_AFTER_SEASONAL - 1; i++) {
            if (level.get(i) == 0) {
                if (new BigDecimal(gamma).equals(new BigDecimal(1))) {
                    seasonalCopy.add(Precision.round(((1 - gamma) * seasonalCopy.get(i)), 7));
                } else {
                    seasonalCopy.add(Precision.round((0.01 * seasonalCopy.get(i)), 7));
                }
                seasonalCopy.add(Precision.round(((1 - gamma) * seasonalCopy.get(i)), 7));
            } else {
                seasonalCopy.add(Precision.round((gamma * values.get(SEASONAL_PERIOD_FOR_WEEK + i)
                        / level.get(i) + (1 - gamma) * seasonalCopy.get(i)), 7));
            }
            if (Math.abs(seasonalCopy.get(seasonalCopy.size() - 1)) < MIN_SEASONAL) {
                if (seasonalCopy.get(seasonalCopy.size() - 1) < 0) {
                    seasonalCopy.set(seasonalCopy.size() - 1, -MIN_SEASONAL);
                } else {
                    seasonalCopy.set(seasonalCopy.size() - 1, MIN_SEASONAL);
                }
            }
            level.add(Precision.round((alpha * values.get(SEASONAL_PERIOD_FOR_WEEK + i + 1)
                    / seasonalCopy.get(i + 1) + (1 - alpha) * (level.get(i) + trend.get(i))), 7));

            trend.add(Precision.round((betta * (level.get(i + 1) - level.get(i)) + (1 - betta) * trend.get(i)), 7));
        }

        // add last seasonal factor
        seasonalCopy.add(Precision.round((gamma * values.get(SEASONAL_PERIOD_FOR_WEEK + EXISTED_PERIOD_AFTER_SEASONAL - 1)
                / level.get(EXISTED_PERIOD_AFTER_SEASONAL - 1)
                + (1 - gamma) * seasonalCopy.get(EXISTED_PERIOD_AFTER_SEASONAL - 1)), 7));

        Map<String, List<Integer>> predictionsFuture = new HashMap<>();

        // define last date of existed data
        String day = LocalDate.parse(cases.lastKey().substring(0, cases.lastKey().indexOf(".")), formatter)
                .plusDays(1).format(formatter);
        String weekDay = LocalDate.parse(cases.lastKey().substring(0, cases.lastKey().indexOf(".")), formatter)
                .plusDays(7).format(formatter);
        int counter = 1;
        // define the most suitable last level and trend factors
        for (int i = 0; i < 10; i++) {
            if (level.get(level.size() - (counter + i)) + trend.get(trend.size() - (counter + i)) > 0) {
                counter += i;
                break;
            }
        }

        int weekCounter = SEASONAL_REPETITION - 1;
        // calculate prediction data and add bounds (confidence interval)
        for (int i = 1; i <= SEASONAL_PERIOD * SEASONAL_REPETITION; i++) {
            List<Integer> predictionRange = new ArrayList<>(3);
            if (day.equals(weekDay)) {
                predictionRange.add(Math.max((int) Precision.round(((level.get(level.size() - counter)
                        + trend.get(trend.size() - counter))
                        * seasonalCopy.get(seasonalCopy.size() - weekCounter-- - 1)), 1), 0));
                predictionRange.add(lowBound(predictionRange.get(0)));
                predictionRange.add(highBound(predictionRange.get(0)));
                weekDay = LocalDate.parse(weekDay, formatter).plusDays(7).format(formatter);
            } else {
                predictionRange.add(0);
                predictionRange.add(0);
                predictionRange.add(0);
            }
            predictionsFuture.put(day + ".csv", predictionRange);

            day = LocalDate.parse(day, formatter).plusDays(1L).format(formatter);
        }

        return predictionsFuture;
    }

    private List<Double> calculateInitialSeasonal(TreeMap<String, Integer> cases) {
        List<Double> list = new ArrayList<>();
        List<Double> seasonal = new ArrayList<>();
        double num = 0;
        int counter = 0;
        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
            if (counter < SEASONAL_PERIOD_FOR_WEEK) {
                num += stringIntegerEntry.getValue();
                seasonal.add(Double.valueOf(stringIntegerEntry.getValue()));
                counter++;
            }
        }
        num = Precision.round(num / (double) SEASONAL_PERIOD_FOR_WEEK, 7);
        for (Double value : seasonal) {
            if (num != 0) {
                if (value == 0) {
                    list.add(0.1);
                    continue;
                }
                list.add(Precision.round(value / num, 7));
            } else {
                list.add(1.0);
            }
        }

        return list;
    }

    public void fillInNonZeroCases(TreeMap<String, Integer> cases, TreeMap<String, Integer> nonZeroCases) {
        List<String> keys = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        String lastNonZeroValue = null;
        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
            keys.add(stringIntegerEntry.getKey());
            values.add(stringIntegerEntry.getValue());
        }
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i) != 0) {
                lastNonZeroValue = keys.get(i);
                break;
            }
        }

        if (lastNonZeroValue == null) {
            nonZeroCases.putAll(cases);
            return;
        }

        while (Utils.dateBefore(keys.get(0), lastNonZeroValue, formatter)) {
            if (cases.get(lastNonZeroValue) == 0) {
                String before = LocalDate.parse(lastNonZeroValue.substring(0, lastNonZeroValue.indexOf(".")), formatter)
                        .minusDays(1).format(formatter) + ".csv";
                String after = LocalDate.parse(lastNonZeroValue.substring(0, lastNonZeroValue.indexOf(".")), formatter)
                        .minusDays(1).format(formatter) + ".csv";
                if (cases.get(before) != 0) {
                    nonZeroCases.put(lastNonZeroValue, cases.get(before));
                } else if (cases.get(after) != 0) {
                    nonZeroCases.put(lastNonZeroValue, cases.get(after));
                } else {
                    nonZeroCases.put(lastNonZeroValue, 0);
                }
            } else {
                nonZeroCases.put(lastNonZeroValue, cases.get(lastNonZeroValue));
            }
            lastNonZeroValue = LocalDate.parse(lastNonZeroValue.substring(0, lastNonZeroValue.indexOf(".")), formatter)
                    .minusDays(7).format(formatter) + ".csv";
        }

//        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
//            if(stringIntegerEntry.getValue() != 0) {
//                nonZeroCases.put(stringIntegerEntry.getKey(), stringIntegerEntry.getValue());
//            }
//        }
//        String day = nonZeroCases.firstKey();
//        while (Utils.dateBefore(day, LocalDate.now().format(formatter) + ".csv", formatter)) {
//            if(!nonZeroCases.containsKey(day) && cases.get(day) != null) {
//                nonZeroCases.put(day, cases.get(day));
//            }
//            day = LocalDate.parse(day.substring(0, day.indexOf(".")), formatter).plusDays(7).format(formatter) + ".csv";
//        }
    }
}
