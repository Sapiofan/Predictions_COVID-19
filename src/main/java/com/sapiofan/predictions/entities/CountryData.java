package com.sapiofan.predictions.entities;

import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class CountryData {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private String country;

    private TreeMap<String, List<Integer>> countryCases = new TreeMap<>(dateComparator());
    private TreeMap<String, List<Integer>> countryConfirmedCases = new TreeMap<>(dateComparator());
    private TreeMap<String, List<Integer>> countryDeaths = new TreeMap<>(dateComparator());
    private TreeMap<String, List<Integer>> countryConfirmedDeaths = new TreeMap<>(dateComparator());

    public CountryData(String country) {
        this.country = country;
    }

    public TreeMap<String, List<Integer>> existedCountryCases() {
        return new TreeMap<>(countryCases.subMap(countryCases.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, List<Integer>> predictedCountryCases() {
        return new TreeMap<>(countryCases.subMap(LocalDate.now().format(formatter),
                true, countryCases.lastKey(), false));
    }

    public TreeMap<String, List<Integer>> existedCountryDeaths() {
        return new TreeMap<>(countryCases.subMap(countryCases.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, List<Integer>> predictedCountryDeaths() {
        return new TreeMap<>(countryCases.subMap(LocalDate.now().format(formatter),
                true, countryCases.lastKey(), false));
    }

    public Map<String, List<Integer>> existedCountryCasesWeekly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, List<Integer>> existedCountryCases = existedCountryCases();

        return calculateCasesWeekly(map, existedCountryCases);
    }

    public Map<String, List<Integer>> predictedCountryCasesWeekly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, List<Integer>> predictedCountryCases = predictedCountryCases();

        return calculateCasesWeekly(map, predictedCountryCases);
    }

    public Map<String, List<Integer>> existedWorldDeathsWeekly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, List<Integer>> existedCountryDeaths = existedCountryCases();

        return calculateCasesWeekly(map, existedCountryDeaths);
    }

    public Map<String, List<Integer>> predictedWorldDeathsWeekly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, List<Integer>> predictedWorldDeaths = predictedCountryDeaths();

        return calculateCasesWeekly(map, predictedWorldDeaths);
    }

    private Map<String, List<Integer>> calculateCasesWeekly(TreeMap<String, List<Integer>> map,
                                                      TreeMap<String, List<Integer>> cases) {
        int counter = 0;
        List<Integer> list = Stream.iterate(0, n -> n).limit(3).collect(Collectors.toList());
        for (Map.Entry<String, List<Integer>> stringMapEntry : cases.entrySet()) {
            list.set(0, list.get(0) + stringMapEntry.getValue().get(0));
            list.set(1, list.get(1) + stringMapEntry.getValue().get(1));
            list.set(2, list.get(2) + stringMapEntry.getValue().get(2));
            if (++counter == 7) {
                counter = 0;
                map.put(stringMapEntry.getKey(), list);
                list = Stream.iterate(0, n -> n).limit(3).collect(Collectors.toList());
            }
        }

        if (counter > 0) {
            map.put(cases.lastKey(), list);
        }

        return map;
    }

    public TreeMap<String, List<Integer>> existedCountryConfirmedCases() {
        return new TreeMap<>(countryConfirmedCases.subMap(countryConfirmedCases.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, List<Integer>> predictedCountryConfirmedCases() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(countryConfirmedCases.subMap(LocalDate.now().format(formatter),
                true, countryConfirmedCases.lastKey(), false));

        for (Map.Entry<String, List<Integer>> stringListEntry : map.entrySet()) {
            List<Integer> list = countryCases.get(stringListEntry.getKey());
            stringListEntry.getValue().add(stringListEntry.getValue().get(0) - list.get(1));
            stringListEntry.getValue().add(stringListEntry.getValue().get(0) + list.get(2));
        }
        return map;
    }

    public TreeMap<String, List<Integer>> existedCountryConfirmedDeaths() {
        return new TreeMap<>(countryConfirmedDeaths.subMap(countryConfirmedDeaths.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, List<Integer>> predictedCountryConfirmedDeaths() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(countryConfirmedDeaths.subMap(LocalDate.now().format(formatter),
                true, countryConfirmedDeaths.lastKey(), false));

        for (Map.Entry<String, List<Integer>> stringListEntry : map.entrySet()) {
            List<Integer> list = countryDeaths.get(stringListEntry.getKey());
            stringListEntry.getValue().add(stringListEntry.getValue().get(0) - list.get(1));
            stringListEntry.getValue().add(stringListEntry.getValue().get(0) + list.get(2));
        }
        return map;
    }

    public TreeMap<String, List<Integer>> existedConfirmedCasesWeakly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, List<Integer>> existedCountryConfirmedCases = existedCountryConfirmedCases();
        calculateConfirmedCasesWeekly(map, existedCountryConfirmedCases);

        return map;
    }

    public TreeMap<String, List<Integer>> predictedConfirmedCasesWeakly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, List<Integer>> predictedCountryConfirmedCases = predictedCountryConfirmedCases();

        calculateConfirmedCasesWeekly(map, predictedCountryConfirmedCases);

        return map;
    }

    private void calculateConfirmedCasesWeekly(TreeMap<String, List<Integer>> confirmedCases,
                                               TreeMap<String, List<Integer>> map) {
        int counter = 0;
        for (Map.Entry<String, List<Integer>> stringMapEntry : confirmedCases.entrySet()) {
            if(counter == 6) {
                map.put(stringMapEntry.getKey(), stringMapEntry.getValue());
                counter = 0;
                continue;
            }
            counter++;
        }
    }

    public TreeMap<String, List<Integer>> existedConfirmedDeathsWeakly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, List<Integer>> existedCountryConfirmedDeaths = existedCountryConfirmedDeaths();

        calculateConfirmedDeathsWeekly(existedCountryConfirmedDeaths, map);

        return map;
    }

    public TreeMap<String, List<Integer>> predictedConfirmedDeathsWeakly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        TreeMap<String, List<Integer>> predictedCountryConfirmedDeaths = predictedCountryConfirmedDeaths();

        calculateConfirmedDeathsWeekly(predictedCountryConfirmedDeaths, map);

        return map;
    }

    private void calculateConfirmedDeathsWeekly(TreeMap<String, List<Integer>> confirmedDeaths,
                                                TreeMap<String, List<Integer>> map) {
        int counter = 0;
        for (Map.Entry<String, List<Integer>> stringMapEntry : confirmedDeaths.entrySet()) {
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
