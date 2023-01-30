package com.sapiofan.predictions.entities;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WorldDataHandler {

    private final WorldData worldData;

    public WorldDataHandler(WorldData worldData) {
        this.worldData = worldData;
    }

    public Integer getTodayConfirmedCases() {
        return worldData.existedConfirmedCases().lastEntry()
                .getValue()
                .entrySet()
                .stream()
                .filter(m -> !m.getKey().equals("World"))
                .findFirst()
                .map(stringListEntry -> stringListEntry.getValue().get(0))
                .orElse(null);
    }

    public Integer getTodayConfirmedDeaths() {
        return worldData.existedConfirmedDeaths().lastEntry()
                .getValue()
                .entrySet()
                .stream()
                .filter(m -> !m.getKey().equals("World"))
                .findFirst()
                .map(stringListEntry -> stringListEntry.getValue().get(0))
                .orElse(null);
    }

    public Integer getTodayNewCases() {
        return worldData.existedWorldCases().lastEntry()
                .getValue()
                .entrySet()
                .stream()
                .filter(m -> !m.getKey().equals("World"))
                .findFirst()
                .map(stringListEntry -> stringListEntry.getValue().get(0))
                .orElse(null);
    }

    public Integer getTodayNewDeaths() {
        return worldData.existedWorldDeaths().lastEntry()
                .getValue()
                .entrySet()
                .stream()
                .filter(m -> !m.getKey().equals("World"))
                .findFirst()
                .map(stringListEntry -> stringListEntry.getValue().get(0))
                .orElse(null);
    }
}
