package com.sapiofan.predictions.controllers;

import com.sapiofan.predictions.services.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @Autowired
    private Statistics statistics;

    @GetMapping("/")
    public String home(Model model) {
        statistics.getWorldStatistics();
        model.addAttribute("smth", "ssss");
        return "home";
    }
}
