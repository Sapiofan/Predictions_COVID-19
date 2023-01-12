package com.sapiofan.predictions.entities;

import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@Data
public class CountryData {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private String country;

    private Map<String, Integer> countryCases = new TreeMap<>(dateComparator());
    private Map<String, Integer> countryDeaths = new TreeMap<>(dateComparator());

//    private Map<String, Integer> countryCasesPrediction = new TreeMap<>(dateComparator());
//    private Map<String, Integer> countryDeathsPrediction = new TreeMap<>(dateComparator());

    public CountryData(String country) {
        this.country = country;
    }

    public Comparator<String> dateComparator() {
        return (o1, o2) -> {
            LocalDate localDate1 = LocalDate.parse(o1.substring(0, o1.indexOf('.')), formatter);
            LocalDate localDate2 = LocalDate.parse(o2.substring(0, o2.indexOf('.')), formatter);
            return localDate1.isAfter(localDate2) ? 1 : -1;
        };
    }
}
