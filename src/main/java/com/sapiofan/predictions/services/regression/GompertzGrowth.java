package com.sapiofan.predictions.services.regression;

import com.sapiofan.predictions.entities.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GompertzGrowth {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private static final Logger log = LoggerFactory.getLogger(GompertzGrowth.class);

    private final int WEEKS = 4;

    private double a = -1, b = -1, c = -1, error = 1_000_000;

    private final TreeMap<String, Integer> confirmedCases;

    public GompertzGrowth(TreeMap<String, Integer> confirmedCases, Data data) {
        this.confirmedCases = new TreeMap<>(data.dateComparator());
        Map.Entry<String, Integer> entry = confirmedCases.firstEntry();
        int init = entry.getValue() % 10_000;
        for (Map.Entry<String, Integer> stringIntegerEntry : confirmedCases.entrySet()) {
            if (stringIntegerEntry.equals(entry)) {
                this.confirmedCases.put(stringIntegerEntry.getKey(), init);
                continue;
            }
            init += (stringIntegerEntry.getValue() - entry.getValue());
            this.confirmedCases.put(stringIntegerEntry.getKey(), init);
            entry = stringIntegerEntry;
        }
    }

    public void predictionCases() {
        binarySearch();
        Map<String, List<Long>> predictionsFuture = new HashMap<>();

        String day = LocalDate.now().format(formatter);
        for (int i = 1; i <= WEEKS * 7; i++) {
            List<Long> predictionRange = new ArrayList<>(3);
            predictionsFuture.put(day + ".csv", predictionRange);
            day = LocalDate.parse(day, formatter).plusDays(1L).format(formatter);
        }
    }

//    private void correction() {
//        double temp = error, res;
//        for (int i = (int) a - 50; i < (int) a + 50; i++) {
//            for (int j = (int) (b * 10) - 50; j < (int) (b * 10) + 50; j++) {
//                for (int k = (int) (c * 10) - 50; k < (int) (c * 10) + 50; k++) {
//                    List<Double> list = new ArrayList<>();
//                    list.add((double) i);
//                    list.add((double) j / 10.0);
//                    list.add((double) k / 10.0);
//                    res = formula(list);
//                    if (res < temp) {
//                        error = res;
//                        a = list.get(0);
//                        b = list.get(1);
//                        c = list.get(2);
//                    }
//
//                }
//            }
//        }
//    }

    public double formula(List<Double> constants, int counter) {
        double a = constants.get(0);
        double b = constants.get(1);
        double c = constants.get(2);
        double sum = 0;
        for (Map.Entry<String, Integer> stringIntegerEntry : confirmedCases.entrySet()) {
            sum += Math.pow(stringIntegerEntry.getValue() - a *
                    Math.exp(-c * Math.exp(-b * (counter))), 2);
            counter++;
        }

        if (!Double.isNaN(sum) && !Double.isInfinite(sum)) {
//            sum = Math.sqrt(sum);
        }

        return Double.isNaN(sum) || Double.isInfinite(sum) ? 2_000_000_000 : sum;
    }

    public double predict(List<Double> constants, int counter) {
        double a = constants.get(0);
        double b = constants.get(1);
        double c = constants.get(2);

        return a * Math.exp(-c * Math.exp(-b * (counter)));
    }

    void binarySearch() {
        List<Double> values = new ArrayList<>(3);
        int l = 0, r = 100000, mid = (l + r) / 2;
        values.add(0, (double) mid);
        values.add(1, 0.0);
        values.add(2, 0.0);
        double a = 0, temp = binarySearchB(values);
        double left, right;

        while (l < r) {
            values.set(0, (double) ((mid + l) / 2));
            left = binarySearchB(values);
            values.set(0, (double) ((mid + r) / 2));
            right = binarySearchB(values);
            if (left > right) {
                l = mid;
                if (right < temp) {
                    temp = right;
                }
            } else {
                r = mid;
                if (left < temp) {
                    temp = left;
                }
            }
            mid = (l + r) / 2;
        }
    }

    double binarySearchB(List<Double> values) {
        List<Double> tempList = new ArrayList<>(values);
        long l = 0, r = 100_000_000l, mid = (l + r) / 2;
        tempList.set(1, mid / 10000.0);
        double b = 0, temp = binarySearchC(tempList);

        double left, right;

        while (l < r) {
            tempList.set(1, (double) ((mid + l) / 2) / 10_000_000.0);
            left = binarySearchC(tempList);
            tempList.set(1, (double) ((mid + r) / 2) / 10_000_000.0);
            right = binarySearchC(tempList);
            if (left > right) {
                l = mid;
                if (right < temp) {
                    temp = right;
                }
            } else {
                r = mid;
                if (left < temp) {
                    temp = left;
                }
            }
            mid = (l + r) / 2;
        }

        return temp;
    }

    double binarySearchC(List<Double> values) {
        List<Double> tempList = new ArrayList<>(values);
        long l = 0, r = 100_000_000l, mid = (l + r) / 2;
        tempList.set(2, mid / 1000000.0);
        double c = 0, temp = formula(tempList, 1);

        double left, right;

        while (l < r) {
            tempList.set(2, (double) ((mid + l) / 2) / 10_000_000.0);
            left = formula(tempList, 1);
            tempList.set(2, (double) ((mid + r) / 2) / 10_000_000.0);
            right = formula(tempList, 1);
            if (left > right) {
                l = mid;
                if (right < temp) {
                    temp = right;
                    c = tempList.get(2);
                }
            } else {
                r = mid;
                if (left < temp) {
                    temp = left;
                    c = (double) ((mid + l) / 2) / 10_000_000.0;
                }
            }
            mid = (l + r) / 2;
        }
        if (temp < error) {
            error = temp;
            this.a = values.get(0);
            this.b = values.get(1);
            this.c = c;
        }

        values.set(2, c);

        return temp;
    }

    public TreeMap<String, Integer> init() {
        TreeMap<String, Integer> map = new TreeMap<>();
        map.put("2003", 46);
        map.put("2004", 72);
        map.put("2005", 112);
        map.put("2006", 167);
        map.put("2007", 239);
        map.put("2008", 324);
        map.put("2009", 412);
        map.put("2010", 494);
        map.put("2011", 561);
        map.put("2012", 612);
        map.put("2013", 648);
        map.put("2014", 671);
        map.put("2015", 686);
        map.put("2016", 696);
        map.put("2017", 702);
        map.put("2018", 705);
        map.put("2019", 708);

        return map;
    }
}
