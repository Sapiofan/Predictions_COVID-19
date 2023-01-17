package com.sapiofan.predictions.entities;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Data {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private Map<String, Map<String, Long>> confirmedCases = new HashMap<>();

    private Map<String, Map<String, Integer>> newCases = new TreeMap<>(dateComparator());

    private Map<String, Map<String, Integer>> predictionNewCases = new TreeMap<>(dateComparator());

    private Map<String, Map<String, Long>> predictionConfirmedCases = new TreeMap<>(dateComparator());

    private Map<String, Map<String, Integer>> deaths = new HashMap<>();

    private Map<String, Map<String, Integer>> newDeaths = new TreeMap<>(dateComparator());

    private Map<String, Map<String, Integer>> predictionNewDeaths = new TreeMap<>(dateComparator());

    private Map<String, Map<String, Integer>> predictionConfirmedDeaths = new TreeMap<>(dateComparator());

    private Map<String, Integer> labelsByDate = new HashMap<>();

    private Map<Integer, String> labelsByNumber = new HashMap<>();


    public Map<String, Map<String, Long>> getConfirmedCases() {
        return confirmedCases;
    }

    public void setConfirmedCases(Map<String, Map<String, Long>> confirmedCases) {
        this.confirmedCases = confirmedCases;
    }

    public Map<String, Map<String, Integer>> getNewCases() {
        return newCases;
    }

    public void setNewCases(Map<String, Map<String, Integer>> newCases) {
        this.newCases = newCases;
    }

    public Map<String, Map<String, Integer>> getDeaths() {
        return deaths;
    }

    public void setDeaths(Map<String, Map<String, Integer>> deaths) {
        this.deaths = deaths;
    }

    public Map<String, Map<String, Integer>> getNewDeaths() {
        return newDeaths;
    }

    public void setNewDeaths(Map<String, Map<String, Integer>> newDeaths) {
        this.newDeaths = newDeaths;
    }

    public Map<String, Integer> getLabelsByDate() {
        return labelsByDate;
    }

    public void setLabelsByDate(Map<String, Integer> labelsByDate) {
        this.labelsByDate = labelsByDate;
    }

    public Map<String, Map<String, Integer>> getPredictionNewCases() {
        return predictionNewCases;
    }

    public void setPredictionNewCases(Map<String, Map<String, Integer>> predictionNewCases) {
        this.predictionNewCases = predictionNewCases;
    }

    public Map<String, Map<String, Integer>> getPredictionNewDeaths() {
        return predictionNewDeaths;
    }

    public void setPredictionNewDeaths(Map<String, Map<String, Integer>> predictionNewDeaths) {
        this.predictionNewDeaths = predictionNewDeaths;
    }

    public Map<Integer, String> getLabelsByNumber() {
        if (labelsByNumber.isEmpty() && !labelsByDate.isEmpty()) {
            labelsByDate.forEach((key, value) -> labelsByNumber.put(value, key));
        }

        return labelsByNumber;
    }

    public Map<String, Map<String, Long>> getPredictionConfirmedCases() {
        return predictionConfirmedCases;
    }

    public void setPredictionConfirmedCases(Map<String, Map<String, Long>> predictionConfirmedCases) {
        this.predictionConfirmedCases = predictionConfirmedCases;
    }

    public Map<String, Map<String, Integer>> getPredictionConfirmedDeaths() {
        return predictionConfirmedDeaths;
    }

    public void setPredictionConfirmedDeaths(Map<String, Map<String, Integer>> predictionConfirmedDeaths) {
        this.predictionConfirmedDeaths = predictionConfirmedDeaths;
    }

    public Comparator<String> dateComparator() {
        return new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                LocalDate localDate1 = LocalDate.parse(o1.substring(0, o1.indexOf('.')), formatter);
                LocalDate localDate2 = LocalDate.parse(o2.substring(0, o2.indexOf('.')), formatter);
                if (localDate1.isAfter(localDate2)) {
                    return 1;
                } else if (localDate1.isEqual(localDate2)) {
                    return 0;
                }

                return -1;
            }
        };
    }
}
