package com.sapiofan.predictions.controllers;

import com.sapiofan.predictions.entities.WorldData;
import com.sapiofan.predictions.services.FileReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController { // add regions confirmed cases for existed data

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private FileReaderService fileReaderService;

    @GetMapping("/")
    public String home(Model model) {
        WorldData worldData = fileReaderService.getWorldData();
        model.addAttribute("initial", worldData.existedWorldDeaths());
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
