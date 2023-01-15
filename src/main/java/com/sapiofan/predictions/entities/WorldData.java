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
public class WorldData {

    private static final Logger log = LoggerFactory.getLogger(WorldData.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private TreeMap<String, Map<String, Integer>> confirmedCases = new TreeMap<>(dateComparator());

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

    public Map<String, Map<String, Integer>> existedWorldCasesWeekly() {
        TreeMap<String, Map<String, Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, Integer>> existedWorldCases = existedWorldCases();

        return calculateCasesWeekly(map, existedWorldCases);
    }

    public Map<String, Map<String, Integer>> predictedWorldCasesWeekly() {
        TreeMap<String, Map<String, Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, Integer>> predictedWorldCases = predictedWorldCases();

        return calculateCasesWeekly(map, predictedWorldCases);
    }

    public Map<String, Map<String, Integer>> existedWorldDeathsWeekly() {
        TreeMap<String, Map<String, Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, Integer>> existedWorldDeaths = existedWorldDeaths();

        return calculateCasesWeekly(map, existedWorldDeaths);
    }

    public Map<String, Map<String, Integer>> predictedWorldDeathsWeekly() {
        TreeMap<String, Map<String, Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, Integer>> predictedWorldDeaths = predictedWorldDeaths();

        return calculateCasesWeekly(map, predictedWorldDeaths);
    }

    private Map<String, Map<String, Integer>> calculateCasesWeekly(TreeMap<String, Map<String,
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
