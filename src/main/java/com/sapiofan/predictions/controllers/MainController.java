package com.sapiofan.predictions.controllers;

import com.sapiofan.predictions.entities.Data;
import com.sapiofan.predictions.services.FileHandlerService;
import com.sapiofan.predictions.services.Statistics;
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
    private Statistics statistics;

    @Autowired
    private FileHandlerService fileHandlerService;

    @GetMapping("/")
    public String home(Model model) {
        Data data = statistics.getWorldData();
        statistics.getWorldStatistics(data);
        TreeMap<String, Map<String, Integer>> sortedMapPrediction = new TreeMap<>(data.dateComparator());
        TreeMap<String, Map<String, Integer>> sortedMapInit = new TreeMap<>(data.dateComparator());
        sortedMapPrediction.putAll(data.getPredictionNewCases());
        sortedMapInit.putAll(data.getNewCases());
        model.addAttribute("prediction", sortedMapPrediction);
        model.addAttribute("initial", sortedMapInit);
        return "home";
    }

    @GetMapping("/load-files")
    public String loadFiles() {
        fileHandlerService.downloadFilesForLastYear();
        return "home";
    }
}
