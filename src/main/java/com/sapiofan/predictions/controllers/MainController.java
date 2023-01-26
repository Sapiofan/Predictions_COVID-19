package com.sapiofan.predictions.controllers;

import com.sapiofan.predictions.entities.AllCountries;
import com.sapiofan.predictions.entities.WorldData;
import com.sapiofan.predictions.services.FileReaderService;
import com.sapiofan.predictions.services.impl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Controller
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private FileReaderService fileReaderService;

    @Autowired
    private Utils utils;

    @GetMapping("/")
    public String home(Model model) {
        WorldData worldData = fileReaderService.getWorldData();
        model.addAttribute("initial", worldData.existedWorldCases());
        model.addAttribute("prediction", worldData.predictedWorldCases());
        model.addAttribute("days", 120);
        model.addAttribute("confirmedCases", 100000);
        model.addAttribute("confirmedDeaths", 28);
        model.addAttribute("predictionDays", 28);
        model.addAttribute("newCases", 28);
        model.addAttribute("newDeaths", 28);
        return "home";
    }

    @GetMapping("/country/{name}")
    public String getCountryStatistics(Model model) {
        model.addAttribute("days", 120);
        model.addAttribute("confirmedCases", 100000);
        model.addAttribute("confirmedDeaths", 28);
        model.addAttribute("predictionDays", 28);
        model.addAttribute("newCases", 28);
        model.addAttribute("newDeaths", 28);
        return "country";
    }

    @GetMapping("/countries")
    public String getCountriesStatistics(Model model) {
        AllCountries allCountries = fileReaderService.getAllCountries();
        TreeMap<String, List<Long>> map = allCountries.getTableView(null);
        map.keySet().retainAll(utils.getCountries());

        model.addAttribute("cases", map);
        model.addAttribute("dates", allCountries.getNewCases());
        model.addAttribute("yesterday", LocalDate.now().minusDays(1)
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        return "countries";
    }

    @GetMapping("/countries/{date}")
    @ResponseBody
    public TreeMap<String, List<Long>> getCountriesByDate(@PathVariable("date") String date) {
        AllCountries allCountries = fileReaderService.getAllCountries();
        TreeMap<String, List<Long>> map = allCountries.getTableView(date);
        map.keySet().retainAll(utils.getCountries());
        return map;
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
