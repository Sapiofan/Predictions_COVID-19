package com.sapiofan.predictions.entities;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
public class WorldData {

    private static final Logger log = LoggerFactory.getLogger(WorldData.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private TreeMap<String, Map<String, List<Integer>>> confirmedCases = new TreeMap<>(dateComparator());

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
        return calculateCasesWeekly(new TreeMap<>(dateComparator()), existedWorldCases());
    }

    public TreeMap<String, Map<String, List<Integer>>> predictedWorldCasesWeekly() {
        return calculateCasesWeekly(new TreeMap<>(dateComparator()), predictedWorldCases());
    }

    public TreeMap<String, Map<String, List<Integer>>> worldCasesWeekly() {
        return calculateCasesWeekly(new TreeMap<>(dateComparator()), worldCases);
    }

    public TreeMap<String, Map<String, List<Integer>>> existedWorldDeathsWeekly() {
        return calculateCasesWeekly(new TreeMap<>(dateComparator()), existedWorldDeaths());
    }

    public TreeMap<String, Map<String, List<Integer>>> predictedWorldDeathsWeekly() {
        return calculateCasesWeekly(new TreeMap<>(dateComparator()), predictedWorldDeaths());
    }

    public TreeMap<String, Map<String, List<Integer>>> worldDeathsWeekly() {
        return calculateCasesWeekly(new TreeMap<>(dateComparator()), worldDeaths);
    }

    private TreeMap<String, Map<String, List<Integer>>> calculateCasesWeekly(TreeMap<String, Map<String, List<Integer>>> map,
                                                                             TreeMap<String, Map<String, List<Integer>>> cases) {

        Map<String, List<Integer>> areas = worldCases.firstEntry()
                .getValue()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, stringListEntry -> IntStream.iterate(0, i -> i)
                        .limit(3).boxed().collect(Collectors.toList()), (a, b) -> b));

        int counter = 0;

        for (Map.Entry<String, Map<String, List<Integer>>> stringMapEntry : cases.entrySet()) {
            for (Map.Entry<String, List<Integer>> stringListEntry : stringMapEntry.getValue().entrySet()) {
                areas.get(stringListEntry.getKey()).set(0, stringListEntry.getValue().get(0)
                        + areas.get(stringListEntry.getKey()).get(0));
                if (stringListEntry.getValue().size() > 1) {
                    areas.get(stringListEntry.getKey()).set(1, stringListEntry.getValue().get(1)
                            + areas.get(stringListEntry.getKey()).get(1));
                    areas.get(stringListEntry.getKey()).set(2, stringListEntry.getValue().get(2)
                            + areas.get(stringListEntry.getKey()).get(2));
                }
            }
            if (++counter == 7) {
                counter = 0;
                map.put(stringMapEntry.getKey(), new HashMap<>(areas));
                areas.entrySet().forEach(stringIntegerEntry -> stringIntegerEntry.setValue(IntStream.iterate(0, i -> i)
                        .limit(3).boxed().collect(Collectors.toList())));
            }
        }

        if (counter > 0) {
            map.put(cases.lastKey(), areas);
        }

        boolean f = false;
        for (Map.Entry<String, Map<String, List<Integer>>> stringMapEntry : map.entrySet()) {
            for (Map.Entry<String, List<Integer>> stringListEntry : stringMapEntry.getValue().entrySet()) {
                if(stringListEntry.getValue().get(2) == 0) {
                    stringListEntry.getValue().set(1, null);
                    stringListEntry.getValue().set(2, null);
                } else {
                    f = true;
                }
            }
            if(f) {
                for (Map.Entry<String, List<Integer>> stringListEntry : stringMapEntry.getValue().entrySet()) {
                    stringListEntry.getValue().set(1, stringListEntry.getValue().get(0));
                    stringListEntry.getValue().set(2, stringListEntry.getValue().get(0));
                }
                break;
            }
        }

        return map;
    }

    public TreeMap<String, Map<String, List<Integer>>> existedConfirmedCases() {
        return new TreeMap<>(confirmedCases.subMap(confirmedCases.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Map<String, List<Integer>>> predictedConfirmedCases() {
        return boundsConfirmedCases(confirmedCases, worldCases);
    }

    public TreeMap<String, Map<String, List<Integer>>> worldConfirmedCases() {
        TreeMap<String, Map<String, List<Integer>>> map = existedConfirmedCases();
        map.putAll(predictedConfirmedCases());

        return map;
    }

    public TreeMap<String, Map<String, List<Integer>>> existedConfirmedDeaths() {
        return new TreeMap<>(confirmedDeaths.subMap(confirmedDeaths.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Map<String, List<Integer>>> predictedConfirmedDeaths() {
        return boundsConfirmedCases(confirmedDeaths, worldDeaths);
    }

    private TreeMap<String, Map<String, List<Integer>>> boundsConfirmedCases(TreeMap<String, Map<String, List<Integer>>> confirmedCases,
                                                                             TreeMap<String, Map<String, List<Integer>>> worldCases) {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(confirmedCases.subMap(LocalDate.now().format(formatter),
                true, confirmedCases.lastKey(), false));
//        for (Map.Entry<String, Map<String, List<Integer>>> stringMapEntry : map.entrySet()) {
//            for (Map.Entry<String, List<Integer>> stringListEntry : stringMapEntry.getValue().entrySet()) {
//                List<Integer> list = worldCases.get(stringMapEntry.getKey()).get(stringListEntry.getKey());
//                stringListEntry.getValue().add(stringListEntry.getValue().get(0) - list.get(1));
//                stringListEntry.getValue().add(stringListEntry.getValue().get(0) + list.get(2));
//            }
//        }

        return map;
    }

    public TreeMap<String, Map<String, List<Integer>>> worldConfirmedDeaths() {
        TreeMap<String, Map<String, List<Integer>>> map = existedConfirmedDeaths();
        map.putAll(predictedConfirmedDeaths());

        return map;
    }

    public TreeMap<String, Map<String, List<Integer>>> existedConfirmedCasesWeakly() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(dateComparator());
        calculateConfirmedCasesWeekly(existedConfirmedCases(), map);

        return map;
    }

    public TreeMap<String, Map<String, List<Integer>>> predictedConfirmedCasesWeakly() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(dateComparator());
        calculateConfirmedCasesWeekly(predictedConfirmedCases(), map);

        return map;
    }

    public TreeMap<String, Map<String, List<Integer>>> worldConfirmedCasesWeakly() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(dateComparator());
        calculateConfirmedCasesWeekly(worldConfirmedCases(), map);

        return map;
    }

    private void calculateConfirmedCasesWeekly(TreeMap<String, Map<String, List<Integer>>> confirmedCases,
                                               TreeMap<String, Map<String, List<Integer>>> map) {
        confirmedCasesWeekly(confirmedCases, map);
    }

    public TreeMap<String, Map<String, List<Integer>>> existedConfirmedDeathsWeakly() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(dateComparator());
        calculateConfirmedDeathsWeekly(existedConfirmedDeaths(), map);

        return map;
    }

    public TreeMap<String, Map<String, List<Integer>>> predictedConfirmedDeathsWeakly() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(dateComparator());
        calculateConfirmedDeathsWeekly(predictedConfirmedDeaths(), map);

        return map;
    }

    public TreeMap<String, Map<String, List<Integer>>> worldConfirmedDeathsWeakly() {
        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(dateComparator());
        calculateConfirmedDeathsWeekly(worldConfirmedDeaths(), map);

        return map;
    }

    private void calculateConfirmedDeathsWeekly(TreeMap<String, Map<String, List<Integer>>> confirmedDeaths,
                                                TreeMap<String, Map<String, List<Integer>>> map) {
        confirmedCasesWeekly(confirmedDeaths, map);
    }

    private void confirmedCasesWeekly(TreeMap<String, Map<String, List<Integer>>> confirmedCases,
                                      TreeMap<String, Map<String, List<Integer>>> map) {
        int counter = 0;
        for (Map.Entry<String, Map<String, List<Integer>>> stringMapEntry : confirmedCases.entrySet()) {
            if (counter == 6) {
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
