package com.sapiofan.predictions.entities;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class WorldData { // confirmed cases (Canada, China)

    private static final Logger log = LoggerFactory.getLogger(WorldData.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private TreeMap<String, Map<String, List<Long>>> confirmedCases = new TreeMap<>(dateComparator());

    private TreeMap<String, Map<String, List<Integer>>> worldCases = new TreeMap<>(dateComparator());

    private TreeMap<String, Map<String, List<Integer>>> confirmedDeaths = new TreeMap<>(dateComparator());

    private TreeMap<String, Map<String, List<Integer>>> worldDeaths = new TreeMap<>(dateComparator());

    public TreeMap<String, Map<String, List<Integer>>> existedWorldCases() {
        return new TreeMap<>(worldCases.subMap(worldCases.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Map<String, List<Integer>>> predictedWorldCases() {
        return new TreeMap<>(worldCases.subMap(LocalDate.now().format(formatter),
                true, worldCases.lastKey(), false));
    }

    public TreeMap<String, Map<String, List<Integer>>> existedWorldDeaths() {
        return new TreeMap<>(worldDeaths.subMap(worldDeaths.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Map<String, List<Integer>>> predictedWorldDeaths() {
        return new TreeMap<>(worldDeaths.subMap(LocalDate.now().format(formatter),
                true, worldDeaths.lastKey(), false));
    }

    public TreeMap<String, Map<String, List<Integer>>> existedWorldCasesWeekly() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, List<Integer>>> existedWorldCases = existedWorldCases();

        return calculateCasesWeekly(map, existedWorldCases);
    }

    public TreeMap<String, Map<String, List<Integer>>> predictedWorldCasesWeekly() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, List<Integer>>> predictedWorldCases = predictedWorldCases();

        return calculateCasesWeekly(map, predictedWorldCases);
    }

    public TreeMap<String, Map<String, List<Integer>>> existedWorldDeathsWeekly() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, List<Integer>>> existedWorldDeaths = existedWorldDeaths();

        return calculateCasesWeekly(map, existedWorldDeaths);
    }

    public TreeMap<String, Map<String, List<Integer>>> predictedWorldDeathsWeekly() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, List<Integer>>> predictedWorldDeaths = predictedWorldDeaths();

        return calculateCasesWeekly(map, predictedWorldDeaths);
    }

    private TreeMap<String, Map<String, List<Integer>>> calculateCasesWeekly(TreeMap<String, Map<String, List<Integer>>> map,
                                                                       TreeMap<String, Map<String, List<Integer>>> cases) {
        Map<String, List<Integer>> areas = worldCases.lastEntry()
                .getValue()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, stringListEntry -> Stream
                        .iterate(0, n -> n)
                        .limit(3)
                        .collect(Collectors.toList()), (a, b) -> b));

        int counter = 0;

        for (Map.Entry<String, Map<String, List<Integer>>> stringMapEntry : cases.entrySet()) {
            for (Map.Entry<String, List<Integer>> stringListEntry : stringMapEntry.getValue().entrySet()) {
                List<Integer> list = new ArrayList<>();
                list.add(stringListEntry.getValue().get(0) + areas.get(stringListEntry.getKey()).get(0));
                list.add(stringListEntry.getValue().get(1) + areas.get(stringListEntry.getKey()).get(1));
                list.add(stringListEntry.getValue().get(2) + areas.get(stringListEntry.getKey()).get(2));
                areas.put(stringListEntry.getKey(), list);
            }
            if (++counter == 7) {
                counter = 0;
                map.put(stringMapEntry.getKey(), new HashMap<>(areas));
                areas.entrySet().forEach(stringIntegerEntry -> stringIntegerEntry.setValue(Stream
                        .iterate(0, n -> n)
                        .limit(3)
                        .collect(Collectors.toList())));
            }
        }

        if (counter > 0) {
            map.put(cases.lastKey(), areas);
        }

        return map;
    }

//    private TreeMap<String, Map<String, Integer>> calculateCasesWeekly(TreeMap<String, Map<String,
//            Integer>> map, TreeMap<String, Map<String, Integer>> cases) {
//        Map<String, Integer> areas = worldCases.lastEntry()
//                .getValue()
//                .entrySet()
//                .stream()
//                .collect(Collectors.toMap(Map.Entry::getKey, stringIntegerEntry -> 0, (a, b) -> b));
//
//        int counter = 0;
//        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : cases.entrySet()) {
//            stringMapEntry.getValue().forEach((key, value) -> areas.put(key, areas.get(key) + value));
//            if (++counter == 7) {
//                counter = 0;
//                map.put(stringMapEntry.getKey(), new HashMap<>(areas));
//                areas.entrySet().forEach(stringIntegerEntry -> stringIntegerEntry.setValue(0));
//            }
//        }
//
//        if (counter > 0) {
//            map.put(cases.lastKey(), areas);
//        }
//
//        return map;
//    }

    public TreeMap<String, Map<String, List<Long>>> existedConfirmedCases() {
        return new TreeMap<>(confirmedCases.subMap(confirmedCases.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Map<String, List<Long>>> predictedConfirmedCases() {
        TreeMap<String, Map<String, List<Long>>> map = new TreeMap<>(confirmedCases.subMap(LocalDate.now().format(formatter),
                true, confirmedCases.lastKey(), false));
        for (Map.Entry<String, Map<String, List<Long>>> stringMapEntry : map.entrySet()) {
            for (Map.Entry<String, List<Long>> stringListEntry : stringMapEntry.getValue().entrySet()) {
                List<Integer> list = worldCases.get(stringMapEntry.getKey()).get(stringListEntry.getKey());
                stringListEntry.getValue().add(stringListEntry.getValue().get(0) - list.get(1));
                stringListEntry.getValue().add(stringListEntry.getValue().get(0) + list.get(2));
            }
        }
        return map;
    }

    public TreeMap<String, Map<String, List<Integer>>> existedConfirmedDeaths() {
        return new TreeMap<>(confirmedDeaths.subMap(confirmedDeaths.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Map<String, List<Integer>>> predictedConfirmedDeaths() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(confirmedDeaths.subMap(LocalDate.now().format(formatter),
                true, confirmedDeaths.lastKey(), false));
        for (Map.Entry<String, Map<String, List<Integer>>> stringMapEntry : map.entrySet()) {
            for (Map.Entry<String, List<Integer>> stringListEntry : stringMapEntry.getValue().entrySet()) {
                List<Integer> list = worldDeaths.get(stringMapEntry.getKey()).get(stringListEntry.getKey());
                stringListEntry.getValue().add(stringListEntry.getValue().get(0) - list.get(1));
                stringListEntry.getValue().add(stringListEntry.getValue().get(0) + list.get(2));
            }
        }

        return map;
    }

    public TreeMap<String, Map<String, List<Long>>> existedConfirmedCasesWeakly() {
        TreeMap<String, Map<String, List<Long>>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, List<Long>>> existedConfirmedCases = existedConfirmedCases();
        calculateConfirmedCasesWeekly(map, existedConfirmedCases);

        return map;
    }

    public TreeMap<String, Map<String, List<Long>>> predictedConfirmedCasesWeakly() {
        TreeMap<String, Map<String, List<Long>>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, List<Long>>> predictedConfirmedCases = predictedConfirmedCases();

        calculateConfirmedCasesWeekly(map, predictedConfirmedCases);

        return map;
    }

    private void calculateConfirmedCasesWeekly(TreeMap<String, Map<String, List<Long>>> confirmedCases,
                                               TreeMap<String, Map<String, List<Long>>> map) {
        int counter = 0;
        for (Map.Entry<String, Map<String, List<Long>>> stringMapEntry : confirmedCases.entrySet()) {
            if(counter == 6) {
                map.put(stringMapEntry.getKey(), stringMapEntry.getValue());
                counter = 0;
                continue;
            }
            counter++;
        }
    }

    public TreeMap<String, Map<String, List<Integer>>> existedConfirmedDeathsWeakly() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, List<Integer>>> existedConfirmedDeaths = existedConfirmedDeaths();

        calculateConfirmedDeathsWeekly(existedConfirmedDeaths, map);

        return map;
    }

    public TreeMap<String, Map<String, List<Integer>>> predictedConfirmedDeathsWeakly() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(dateComparator());
        TreeMap<String, Map<String, List<Integer>>> predictedConfirmedDeaths = predictedConfirmedDeaths();

        calculateConfirmedDeathsWeekly(predictedConfirmedDeaths, map);

        return map;
    }

    private void calculateConfirmedDeathsWeekly(TreeMap<String, Map<String, List<Integer>>> confirmedDeaths,
                                               TreeMap<String, Map<String, List<Integer>>> map) {
        int counter = 0;
        for (Map.Entry<String, Map<String, List<Integer>>> stringMapEntry : confirmedDeaths.entrySet()) {
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
