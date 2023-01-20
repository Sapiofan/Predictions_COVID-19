package com.sapiofan.predictions.entities;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Data
public class WorldData { // confirmed cases

    private static final Logger log = LoggerFactory.getLogger(WorldData.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private TreeMap<String, Map<String, Long>> confirmedCases = new TreeMap<>(dateComparator());

    private TreeMap<String, Map<String, Integer>> worldCases = new TreeMap<>(dateComparator());

    private TreeMap<String, Map<String, Integer>> confirmedDeaths = new TreeMap<>(dateComparator());

    private TreeMap<String, Map<String, Integer>> worldDeaths = new TreeMap<>(dateComparator());

    public TreeMap<String, Map<String, Integer>> existedWorldCases() {
        return new TreeMap<>(worldCases.subMap(worldCases.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Map<String, Integer>> predictedWorldCases() {
        return new TreeMap<>(worldCases.subMap(LocalDate.now().format(formatter),
                true, worldCases.lastKey(), false));
    }

    public TreeMap<String, Map<String, Integer>> existedWorldDeaths() {
        return new TreeMap<>(worldDeaths.subMap(worldDeaths.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Map<String, Integer>> predictedWorldDeaths() {
        return new TreeMap<>(worldDeaths.subMap(LocalDate.now().format(formatter),
                true, worldDeaths.lastKey(), false));
    }

    public TreeMap<String, Map<String, Integer>> existedWorldCasesWeekly() {
        TreeMap<String, Map<String, Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, Integer>> existedWorldCases = existedWorldCases();

        return calculateCasesWeekly(map, existedWorldCases);
    }

    public TreeMap<String, Map<String, Integer>> predictedWorldCasesWeekly() {
        TreeMap<String, Map<String, Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, Integer>> predictedWorldCases = predictedWorldCases();

        return calculateCasesWeekly(map, predictedWorldCases);
    }

    public TreeMap<String, Map<String, Integer>> existedWorldDeathsWeekly() {
        TreeMap<String, Map<String, Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, Integer>> existedWorldDeaths = existedWorldDeaths();

        return calculateCasesWeekly(map, existedWorldDeaths);
    }

    public TreeMap<String, Map<String, Integer>> predictedWorldDeathsWeekly() {
        TreeMap<String, Map<String, Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, Integer>> predictedWorldDeaths = predictedWorldDeaths();

        return calculateCasesWeekly(map, predictedWorldDeaths);
    }

    private TreeMap<String, Map<String, Integer>> calculateCasesWeekly(TreeMap<String, Map<String,
            Integer>> map, TreeMap<String, Map<String, Integer>> cases) {
        Map<String, Integer> areas = worldCases.lastEntry()
                .getValue()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, stringIntegerEntry -> 0, (a, b) -> b));

        int counter = 0;
        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : cases.entrySet()) {
            stringMapEntry.getValue().forEach((key, value) -> areas.put(key, areas.get(key) + value));
            if (++counter == 7) {
                counter = 0;
                map.put(stringMapEntry.getKey(), new HashMap<>(areas));
                areas.entrySet().forEach(stringIntegerEntry -> stringIntegerEntry.setValue(0));
            }
        }

        if (counter > 0) {
            map.put(cases.lastKey(), areas);
        }

        return map;
    }

    public TreeMap<String, Map<String, Long>> existedConfirmedCases() {
        return new TreeMap<>(confirmedCases.subMap(confirmedCases.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Map<String, Long>> predictedConfirmedCases() {
        return new TreeMap<>(confirmedCases.subMap(LocalDate.now().format(formatter),
                true, confirmedCases.lastKey(), false));
    }

    public TreeMap<String, Map<String, Integer>> existedConfirmedDeaths() {
        return new TreeMap<>(confirmedDeaths.subMap(confirmedDeaths.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Map<String, Integer>> predictedConfirmedDeaths() {
        return new TreeMap<>(confirmedDeaths.subMap(LocalDate.now().format(formatter),
                true, confirmedDeaths.lastKey(), false));
    }

    public TreeMap<String, Map<String, Long>> existedConfirmedCasesWeakly() {
        TreeMap<String, Map<String, Long>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, Long>> existedConfirmedCases = existedConfirmedCases();
        calculateConfirmedCasesWeekly(map, existedConfirmedCases);

        return map;
    }

    public TreeMap<String, Map<String, Long>> predictedConfirmedCasesWeakly() {
        TreeMap<String, Map<String, Long>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, Long>> predictedConfirmedCases = predictedConfirmedCases();

        calculateConfirmedCasesWeekly(map, predictedConfirmedCases);

        return map;
    }

    private void calculateConfirmedCasesWeekly(TreeMap<String, Map<String, Long>> confirmedCases,
                                               TreeMap<String, Map<String, Long>> map) {
        int counter = 0;
        for (Map.Entry<String, Map<String, Long>> stringMapEntry : confirmedCases.entrySet()) {
            if(counter == 6) {
                map.put(stringMapEntry.getKey(), stringMapEntry.getValue());
                counter = 0;
                continue;
            }
            counter++;
        }
    }

    public TreeMap<String, Map<String, Integer>> existedConfirmedDeathsWeakly() {
        TreeMap<String, Map<String, Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, Integer>> existedConfirmedDeaths = existedConfirmedDeaths();

        calculateConfirmedDeathsWeekly(existedConfirmedDeaths, map);

        return map;
    }

    public TreeMap<String, Map<String, Integer>> predictedConfirmedDeathsWeakly() {
        TreeMap<String, Map<String, Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, Integer>> predictedConfirmedDeaths = predictedConfirmedDeaths();

        calculateConfirmedDeathsWeekly(predictedConfirmedDeaths, map);

        return map;
    }

    private void calculateConfirmedDeathsWeekly(TreeMap<String, Map<String, Integer>> confirmedDeaths,
                                               TreeMap<String, Map<String, Integer>> map) {
        int counter = 0;
        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : confirmedDeaths.entrySet()) {
            if(counter == 6) {
                map.put(stringMapEntry.getKey(), stringMapEntry.getValue());
                counter = 0;
                continue;
            }
            counter++;
        }
    }

    public Comparator<String> dateComparator() {
        return (o1, o2) -> {
            LocalDate localDate1 = LocalDate.parse(o1, formatter);
            LocalDate localDate2 = LocalDate.parse(o2, formatter);
            if (localDate1.isAfter(localDate2)) {
                return 1;
            } else if (localDate1.isEqual(localDate2)) {
                return 0;
            }

            return -1;
        };
    }
}
