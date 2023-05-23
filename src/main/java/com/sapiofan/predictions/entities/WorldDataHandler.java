package com.sapiofan.predictions.entities;

import java.util.List;
import java.util.Map;

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
        final String[] lastDay = new String[1];
        worldData.existedConfirmedCases().entrySet()
                .stream()
                .takeWhile(stringMapEntry -> stringMapEntry.getValue().values()
                        .stream().findFirst().get().size() <= 1)
                .forEach(stringMapEntry -> lastDay[0] = stringMapEntry.getKey());
        return lastDay[0];
    }

    public String getLastDate() {
        return worldData.worldConfirmedCases().lastEntry().getKey();
    }
}
