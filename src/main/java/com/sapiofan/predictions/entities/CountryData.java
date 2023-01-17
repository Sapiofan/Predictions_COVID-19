package com.sapiofan.predictions.entities;

import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@Data
public class CountryData {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private String country;

    private TreeMap<String, Integer> countryCases = new TreeMap<>(dateComparator());
    private TreeMap<String, Integer> countryConfirmedCases = new TreeMap<>(dateComparator());
    private TreeMap<String, Integer> countryDeaths = new TreeMap<>(dateComparator());
    private TreeMap<String, Integer> countryConfirmedDeaths = new TreeMap<>(dateComparator());

    public CountryData(String country) {
        this.country = country;
    }

    public TreeMap<String, Integer> existedCountryCases() {
        return new TreeMap<>(countryCases.subMap(countryCases.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Integer> predictedCountryCases() {
        return new TreeMap<>(countryCases.subMap(LocalDate.now().format(formatter),
                true, countryCases.lastKey(), false));
    }

    public TreeMap<String, Integer> existedCountryDeaths() {
        return new TreeMap<>(countryCases.subMap(countryCases.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Integer> predictedCountryDeaths() {
        return new TreeMap<>(countryCases.subMap(LocalDate.now().format(formatter),
                true, countryCases.lastKey(), false));
    }

    public Map<String, Integer> existedCountryCasesWeekly() {
        TreeMap<String, Integer> map = new TreeMap<>(dateComparator());
        TreeMap<String, Integer> existedCountryCases = existedCountryCases();

        return calculateCasesWeekly(map, existedCountryCases);
    }

    public Map<String, Integer> predictedCountryCasesWeekly() {
        TreeMap<String, Integer> map = new TreeMap<>(dateComparator());
        TreeMap<String, Integer> predictedCountryCases = predictedCountryCases();

        return calculateCasesWeekly(map, predictedCountryCases);
    }

    public Map<String, Integer> existedWorldDeathsWeekly() {
        TreeMap<String, Integer> map = new TreeMap<>(dateComparator());
        TreeMap<String, Integer> existedCountryDeaths = existedCountryCases();

        return calculateCasesWeekly(map, existedCountryDeaths);
    }

    public Map<String, Integer> predictedWorldDeathsWeekly() {
        TreeMap<String, Integer> map = new TreeMap<>(dateComparator());
        TreeMap<String, Integer> predictedWorldDeaths = predictedCountryDeaths();

        return calculateCasesWeekly(map, predictedWorldDeaths);
    }

    private Map<String, Integer> calculateCasesWeekly(TreeMap<String, Integer> map,
                                                      TreeMap<String, Integer> cases) {
        int counter = 0, sum = 0;
        for (Map.Entry<String, Integer> stringMapEntry : cases.entrySet()) {
            sum += stringMapEntry.getValue();
            if (++counter == 7) {
                counter = 0;
                map.put(stringMapEntry.getKey(), sum);
                sum = 0;
            }
        }

        if (counter > 0) {
            map.put(cases.lastKey(), sum);
        }

        return map;
    }

    public TreeMap<String, Integer> existedCountryConfirmedCases() {
        return new TreeMap<>(countryConfirmedCases.subMap(countryConfirmedCases.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Integer> predictedCountryConfirmedCases() {
        return new TreeMap<>(countryConfirmedCases.subMap(LocalDate.now().format(formatter),
                true, countryConfirmedCases.lastKey(), false));
    }

    public TreeMap<String, Integer> existedCountryConfirmedDeaths() {
        return new TreeMap<>(countryConfirmedDeaths.subMap(countryConfirmedDeaths.firstKey(),
                true, LocalDate.now().format(formatter), false));
    }

    public TreeMap<String, Integer> predictedCountryConfirmedDeaths() {
        return new TreeMap<>(countryConfirmedDeaths.subMap(LocalDate.now().format(formatter),
                true, countryConfirmedDeaths.lastKey(), false));
    }

    public TreeMap<String, Integer> existedConfirmedCasesWeakly() {
        TreeMap<String, Integer> map = new TreeMap<>(dateComparator());
        TreeMap<String, Integer> existedCountryConfirmedCases = existedCountryConfirmedCases();
        calculateConfirmedCasesWeekly(map, existedCountryConfirmedCases);

        return map;
    }

    public TreeMap<String, Integer> predictedConfirmedCasesWeakly() {
        TreeMap<String, Integer> map = new TreeMap<>(dateComparator());
        TreeMap<String, Integer> predictedCountryConfirmedCases = predictedCountryConfirmedCases();

        calculateConfirmedCasesWeekly(map, predictedCountryConfirmedCases);

        return map;
    }

    private void calculateConfirmedCasesWeekly(TreeMap<String, Integer> confirmedCases,
                                               TreeMap<String, Integer> map) {
        int counter = 0;
        for (Map.Entry<String, Integer> stringMapEntry : confirmedCases.entrySet()) {
            if(counter == 6) {
                map.put(stringMapEntry.getKey(), stringMapEntry.getValue());
                counter = 0;
                continue;
            }
            counter++;
        }
    }

    public TreeMap<String, Integer> existedConfirmedDeathsWeakly() {
        TreeMap<String, Integer> map = new TreeMap<>(dateComparator());
        TreeMap<String, Integer> existedCountryConfirmedDeaths = existedCountryConfirmedDeaths();

        calculateConfirmedDeathsWeekly(existedCountryConfirmedDeaths, map);

        return map;
    }

    public TreeMap<String, Integer> predictedConfirmedDeathsWeakly() {
        TreeMap<String, Integer> map = new TreeMap<>(dateComparator());
        TreeMap<String, Integer> predictedCountryConfirmedDeaths = predictedCountryConfirmedDeaths();

        calculateConfirmedDeathsWeekly(predictedCountryConfirmedDeaths, map);

        return map;
    }

    private void calculateConfirmedDeathsWeekly(TreeMap<String, Integer> confirmedDeaths,
                                                TreeMap<String, Integer> map) {
        int counter = 0;
        for (Map.Entry<String, Integer> stringMapEntry : confirmedDeaths.entrySet()) {
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
