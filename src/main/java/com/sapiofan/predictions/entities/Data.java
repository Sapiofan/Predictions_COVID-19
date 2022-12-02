package com.sapiofan.predictions.entities;

import java.util.HashMap;
import java.util.Map;

public class Data {

    private Map<String, Map<String, Integer>> confirmedCases = new HashMap<>();

    private Map<String, Map<String, Integer>> newCases = new HashMap<>();

    private Map<String, Map<String, Integer>> predictionNewCases = new HashMap<>();

    private Map<String, Map<String, Integer>> deaths = new HashMap<>();

    private Map<String, Map<String, Integer>> newDeaths = new HashMap<>();

    private Map<String, Map<String, Integer>> predictionNewDeaths = new HashMap<>();

    private Map<String, Integer> labels = new HashMap<>();


    public Map<String, Map<String, Integer>> getConfirmedCases() {
        return confirmedCases;
    }

    public void setConfirmedCases(Map<String, Map<String, Integer>> confirmedCases) {
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

    public Map<String, Integer> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, Integer> labels) {
        this.labels = labels;
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
}
