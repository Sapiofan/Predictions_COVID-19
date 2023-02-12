package com.sapiofan.predictions.entities;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Data
public class AllCountries {

    private static final Logger log = LoggerFactory.getLogger(AllCountries.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private TreeMap<String, Map<String, Long>> confirmedCases = new TreeMap<>(dateComparator());

    private TreeMap<String, Map<String, Integer>> newCases = new TreeMap<>(dateComparator());

    private TreeMap<String, Map<String, Integer>> confirmedDeaths = new TreeMap<>(dateComparator());

    private TreeMap<String, Map<String, Integer>> newDeaths = new TreeMap<>(dateComparator());

    public TreeMap<String, List<Long>> getTableView(String date) {
        TreeMap<String, List<Long>> map = new TreeMap<>();

        Map<String, Long> cc = getDayCases(date);

        Map<String, Integer> nc = getDayNewCases(date);

        Map<String, Integer> cd = getDayDeaths(date);

        Map<String, Integer> nd = getDayNewDeaths(date);

        for (Map.Entry<String, Long> stringLongEntry : cc.entrySet()) {
            List<Long> list = new ArrayList<>();
            list.add(stringLongEntry.getValue());
            list.add(Long.valueOf(nc.get(stringLongEntry.getKey())));
            list.add(Long.valueOf(cd.get(stringLongEntry.getKey())));
            list.add(Long.valueOf(nd.get(stringLongEntry.getKey())));
            map.put(stringLongEntry.getKey(), list);
        }

        return map;
    }

    public Map<String, Long> getDayCases(String date) {
        if (date == null) {
            date = LocalDate.now().format(formatter);
        }
        String finalDate = date;

        return confirmedCases.entrySet()
                .stream()
                .filter(stringMapEntry -> stringMapEntry.getKey().equals(finalDate)).findFirst()
                .get()
                .getValue();
    }

    public Map<String, Integer> getDayNewCases(String date) {
        if (date == null) {
            date = LocalDate.now().format(formatter);
        }
        String finalDate = date;

        return newCases.entrySet()
                .stream()
                .filter(stringMapEntry -> stringMapEntry.getKey().equals(finalDate)).findFirst()
                .get()
                .getValue();
    }

    public Map<String, Integer> getDayDeaths(String date) {
        if (date == null) {
            date = LocalDate.now().format(formatter);
        }
        String finalDate = date;

        return confirmedDeaths.entrySet()
                .stream()
                .filter(stringMapEntry -> stringMapEntry.getKey().equals(finalDate)).findFirst()
                .get()
                .getValue();
    }

    public Map<String, Integer> getDayNewDeaths(String date) {
        if (date == null) {
            date = LocalDate.now().format(formatter);
        }
        String finalDate = date;

        return newDeaths.entrySet()
                .stream()
                .filter(stringMapEntry -> stringMapEntry.getKey().equals(finalDate)).findFirst()
                .get()
                .getValue();
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
