package com.sapiofan.predictions.controllers;

import com.sapiofan.predictions.entities.Data;
import com.sapiofan.predictions.entities.WorldData;
import com.sapiofan.predictions.services.FileHandlerService;
import com.sapiofan.predictions.services.FileReaderService;
import com.sapiofan.predictions.services.Statistics;
import com.sapiofan.predictions.services.impl.FileReaderServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;
import java.util.TreeMap;

@Controller
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private FileReaderService fileReaderService;

    @GetMapping("/")
    public String home(Model model) {
//        Data data = statistics.getWorldData();
//        statistics.getWorldStatistics(data);
//        TreeMap<String, Map<String, Integer>> sortedMapPrediction = new TreeMap<>(data.dateComparator());
//        TreeMap<String, Map<String, Integer>> sortedMapInit = new TreeMap<>(data.dateComparator());
//        sortedMapPrediction.putAll(data.getPredictionNewCases());
//        sortedMapInit.putAll(data.getNewCases());
        WorldData worldData = fileReaderService.getWorldData();
        Map<String, Integer> world = new TreeMap<>(worldData.dateComparator());
        worldData.getWorldCases().forEach((key, value) -> value.entrySet()
                .stream()
                .filter(stringIntegerEntry -> stringIntegerEntry.getKey().equals("World"))
                .forEach(stringIntegerEntry -> world.put(key, stringIntegerEntry.getValue())));
//        model.addAttribute("prediction", sortedMapPrediction);
//        model.addAttribute("initial", sortedMapInit);
        model.addAttribute("initial", world);
        return "home";
    }

    @GetMapping("/country")
    public String getCountryStatistics() {
        return "country";
    }

    @GetMapping("/countries")
    public String getCountriesStatistics() {

        return "country";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
