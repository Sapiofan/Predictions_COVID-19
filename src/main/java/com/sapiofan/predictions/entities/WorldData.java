package com.sapiofan.predictions.entities;

import com.sapiofan.predictions.controllers.MainController;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

@Data
public class WorldData {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private TreeMap<String, Map<String, Integer>> worldCases = new TreeMap<>(dateComparator());

    private TreeMap<String, Map<String, Integer>> worldDeaths = new TreeMap<>(dateComparator());

    public Map<String, Map<String, Integer>> existedWorldCases() {
        return worldCases.subMap(worldCases.firstKey(), true, LocalDate.now().format(formatter), false);
    }

    public Map<String, Map<String, Integer>> predictedWorldCases() {
        return worldCases.subMap(LocalDate.now().format(formatter), true, worldCases.lastKey(), false);
    }

    public Map<String, Map<String, Integer>> existedWorldDeaths() {
        return worldDeaths.subMap(worldDeaths.firstKey(), true, LocalDate.now().format(formatter), false);
    }

    public Map<String, Map<String, Integer>> predictedWorldDeaths() {
        return worldDeaths.subMap(LocalDate.now().format(formatter), true, worldDeaths.lastKey(), false);
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
