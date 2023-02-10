package com.sapiofan.predictions.entities;

public class WorldDataHandler {

    private final WorldData worldData;

    public WorldDataHandler(WorldData worldData) {
        this.worldData = worldData;
    }

    public Integer getTodayConfirmedCases(String region) {
        return worldData.existedConfirmedCases().lastEntry()
                .getValue()
                .entrySet()
                .stream()
                .filter(m -> m.getKey().equals(region))
                .findFirst()
                .map(stringListEntry -> stringListEntry.getValue().get(0))
                .orElse(null);
    }

    public Integer getTodayConfirmedDeaths(String region) {
        return worldData.existedConfirmedDeaths().lastEntry()
                .getValue()
                .entrySet()
                .stream()
                .filter(m -> m.getKey().equals(region))
                .findFirst()
                .map(stringListEntry -> stringListEntry.getValue().get(0))
                .orElse(null);
    }

    public Integer getPredictedCases(String region) {
        return worldData.predictedConfirmedCases().lastEntry()
                .getValue()
                .entrySet()
                .stream()
                .filter(m -> m.getKey().equals(region))
                .findFirst()
                .map(stringListEntry -> stringListEntry.getValue().get(0))
                .orElse(null);
    }

    public Integer getPredictedDeaths(String region) {
        return worldData.predictedConfirmedDeaths().lastEntry()
                .getValue()
                .entrySet()
                .stream()
                .filter(m -> m.getKey().equals(region))
                .findFirst()
                .map(stringListEntry -> stringListEntry.getValue().get(0))
                .orElse(null);
    }

    public String getLastExistedDate() {
        return worldData.existedConfirmedCases().lastEntry().getKey();
    }

    public String getLastDate() {
        return worldData.worldConfirmedCases().lastEntry().getKey();
    }
}
