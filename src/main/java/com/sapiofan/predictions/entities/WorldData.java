package com.sapiofan.predictions.entities;

import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@Data
public class WorldData {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private Map<String, Map<String, Integer>> worldCases = new TreeMap<>(dateComparator());

    private Map<String, Map<String, Integer>> worldDeaths = new TreeMap<>(dateComparator());

    public Comparator<String> dateComparator() {
        return (o1, o2) -> {
            LocalDate localDate1 = LocalDate.parse(o1.substring(0, o1.indexOf('.')), formatter);
            LocalDate localDate2 = LocalDate.parse(o2.substring(0, o2.indexOf('.')), formatter);
            return localDate1.isAfter(localDate2) ? 1 : -1;
        };
    }
}
