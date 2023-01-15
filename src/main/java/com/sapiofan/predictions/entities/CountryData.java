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

    public Map<String, Integer> existedWorldCases() {
        return countryCases.subMap(countryCases.firstKey(), true, LocalDate.now().format(formatter), false);
    }

    public Map<String, Integer> predictedWorldCases() {
        return countryCases.subMap(LocalDate.now().format(formatter), true, countryCases.lastKey(), false);
    }

    public Map<String, Integer> existedWorldDeaths() {
        return countryCases.subMap(countryCases.firstKey(), true, LocalDate.now().format(formatter), false);
    }

    public Map<String, Integer> predictedWorldDeaths() {
        return countryCases.subMap(LocalDate.now().format(formatter), true, countryCases.lastKey(), false);
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
