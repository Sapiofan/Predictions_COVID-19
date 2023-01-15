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
    private TreeMap<String, Integer> countryDeaths = new TreeMap<>(dateComparator());

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
