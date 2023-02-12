package com.sapiofan.predictions.entities;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
public class CountryData {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final Logger log = LoggerFactory.getLogger(CountryData.class);

    private String country;

    private TreeMap<String, List<Integer>> countryCases = new TreeMap<>(dateComparator());
    private TreeMap<String, List<Integer>> countryConfirmedCases = new TreeMap<>(dateComparator());
    private TreeMap<String, List<Integer>> countryDeaths = new TreeMap<>(dateComparator());
    private TreeMap<String, List<Integer>> countryConfirmedDeaths = new TreeMap<>(dateComparator());

    public CountryData(String country) {
        this.country = country;
    }

    public TreeMap<String, List<Integer>> existedCountryCases() {
        return boundsOfExistedCases(countryCases);
    }

    public TreeMap<String, List<Integer>> predictedCountryCases() {
        return new TreeMap<>(countryCases.subMap(LocalDate.now().format(formatter),
                true, countryCases.lastKey(), false));
    }

    public TreeMap<String, List<Integer>> existedCountryDeaths() {
        return boundsOfExistedCases(countryDeaths);
    }

    private TreeMap<String, List<Integer>> boundsOfExistedCases(TreeMap<String, List<Integer>> countryCases) {
        TreeMap<String, List<Integer>> map = new TreeMap<>(countryCases.subMap(countryCases.firstKey(),
                true, LocalDate.now().format(formatter), false));
        map.lastEntry().getValue().add(map.lastEntry().getValue().get(0));
        map.lastEntry().getValue().add(map.lastEntry().getValue().get(0));
        return map;
    }

    public TreeMap<String, List<Integer>> predictedCountryDeaths() {
        return new TreeMap<>(countryDeaths.subMap(LocalDate.now().format(formatter),
                true, countryDeaths.lastKey(), false));
    }

    public TreeMap<String, List<Integer>> existedCountryCasesWeekly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());

        return calculateCasesWeekly(map, existedCountryCases());
    }

    public TreeMap<String, List<Integer>> predictedCountryCasesWeekly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());

        return calculateCasesWeekly(map, predictedCountryCases());
    }

    public TreeMap<String, List<Integer>> countryCasesWeekly() {
        return calculateCasesWeekly(new TreeMap<>(dateComparator()), countryCases);
    }

    public TreeMap<String, List<Integer>> existedCountryDeathsWeekly() {
        return calculateCasesWeekly(new TreeMap<>(dateComparator()), existedCountryDeaths());
    }

    public TreeMap<String, List<Integer>> predictedCountryDeathsWeekly() {
        return calculateCasesWeekly(new TreeMap<>(dateComparator()), predictedCountryDeaths());
    }

    public TreeMap<String, List<Integer>> countryDeathsWeekly() {
        return calculateCasesWeekly(new TreeMap<>(dateComparator()), countryDeaths);
    }

    private TreeMap<String, List<Integer>> calculateCasesWeekly(TreeMap<String, List<Integer>> map,
                                                                TreeMap<String, List<Integer>> cases) {
        int counter = 0;
        List<Integer> list = IntStream.iterate(0, i -> i).limit(3).boxed().collect(Collectors.toList());

        for (Map.Entry<String, List<Integer>> stringMapEntry : cases.entrySet()) {
            list.set(0, list.get(0) + stringMapEntry.getValue().get(0));
            if (stringMapEntry.getValue().size() > 1) {
                list.set(1, list.get(1) + stringMapEntry.getValue().get(1));
                list.set(2, list.get(2) + stringMapEntry.getValue().get(2));
            } else {
                list.set(1, list.get(1) + stringMapEntry.getValue().get(0));
                list.set(2, list.get(2) + stringMapEntry.getValue().get(0));
            }
            if (++counter == 7) {
                counter = 0;
                map.put(stringMapEntry.getKey(), list);
                list = IntStream.iterate(0, i -> i).limit(3).boxed().collect(Collectors.toList());
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
        return addBoundsOfConfirmedCases(countryConfirmedCases, countryCases);
    }

    public TreeMap<String, List<Integer>> countryConfirmedCases() {
        TreeMap<String, List<Integer>> map = existedCountryConfirmedCases();
        map.putAll(predictedCountryConfirmedCases());

        return map;
    }

    public TreeMap<String, List<Integer>> existedCountryConfirmedDeaths() {
        return new TreeMap<>(countryConfirmedDeaths.subMap(countryConfirmedDeaths.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, List<Integer>> predictedCountryConfirmedDeaths() {
        return addBoundsOfConfirmedCases(countryConfirmedDeaths, countryDeaths);
    }

    private TreeMap<String, List<Integer>> addBoundsOfConfirmedCases(TreeMap<String, List<Integer>> countryConfirmedCases,
                                                                     TreeMap<String, List<Integer>> countryCases) {
        TreeMap<String, List<Integer>> map = new TreeMap<>(countryConfirmedCases.subMap(LocalDate.now().format(formatter),
                true, countryConfirmedCases.lastKey(), false));

        for (Map.Entry<String, List<Integer>> stringListEntry : map.entrySet()) {
            List<Integer> list = countryCases.get(stringListEntry.getKey());
            stringListEntry.getValue().add(stringListEntry.getValue().get(0) - list.get(1));
            stringListEntry.getValue().add(stringListEntry.getValue().get(0) + list.get(2));
        }

        return map;
    }

    public TreeMap<String, List<Integer>> countryConfirmedDeaths() {
        TreeMap<String, List<Integer>> map = existedCountryConfirmedDeaths();
        map.putAll(predictedCountryConfirmedDeaths());
        return map;
    }

    public TreeMap<String, List<Integer>> existedConfirmedCasesWeakly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        calculateConfirmedCasesWeekly(existedCountryConfirmedCases(), map);

        return map;
    }

    public TreeMap<String, List<Integer>> predictedConfirmedCasesWeakly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        calculateConfirmedCasesWeekly(predictedCountryConfirmedCases(), map);

        return map;
    }

    public TreeMap<String, List<Integer>> confirmedCasesWeakly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        calculateConfirmedCasesWeekly(countryConfirmedCases(), map);

        return map;
    }

    private void calculateConfirmedCasesWeekly(TreeMap<String, List<Integer>> confirmedCases,
                                               TreeMap<String, List<Integer>> map) {
        confirmedWeekly(confirmedCases, map);
    }

    public TreeMap<String, List<Integer>> existedConfirmedDeathsWeakly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        calculateConfirmedDeathsWeekly(existedCountryConfirmedDeaths(), map);

        return map;
    }

    public TreeMap<String, List<Integer>> predictedConfirmedDeathsWeakly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        calculateConfirmedDeathsWeekly(predictedCountryConfirmedDeaths(), map);

        return map;
    }

    public TreeMap<String, List<Integer>> confirmedDeathsWeakly() {
        TreeMap<String, List<Integer>> map = new TreeMap<>(dateComparator());
        calculateConfirmedDeathsWeekly(countryConfirmedDeaths(), map);

        return map;
    }

    private void calculateConfirmedDeathsWeekly(TreeMap<String, List<Integer>> confirmedDeaths,
                                                TreeMap<String, List<Integer>> map) {
        confirmedWeekly(confirmedDeaths, map);
    }

    private void confirmedWeekly(TreeMap<String, List<Integer>> confirmedCases, TreeMap<String, List<Integer>> map) {
        int counter = 0;
        for (Map.Entry<String, List<Integer>> stringMapEntry : confirmedCases.entrySet()) {
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
