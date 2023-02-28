package com.sapiofan.predictions.controllers;

import com.sapiofan.predictions.entities.*;
import com.sapiofan.predictions.services.FileReaderService;
import com.sapiofan.predictions.services.impl.DBHandlerService;
import com.sapiofan.predictions.services.impl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Controller
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private FileReaderService fileReaderService;

    @Autowired
    private DBHandlerService dbHandlerService;

    @Autowired
    private Utils utils;

    @GetMapping("/")
    public String home(Model model) {
//        WorldData worldData = fileReaderService.getWorldData();
        WorldData worldData = dbHandlerService.worldData(180);
        WorldDataHandler worldDataHandler = new WorldDataHandler(worldData);
        TreeMap<String, Map<String, List<Integer>>> worldCases = worldData.getWorldCases();
        TreeMap<String, Map<String, List<Integer>>> worldDeaths = worldData.getWorldDeaths();
        model.addAttribute("cases", worldCases);
        model.addAttribute("deaths", worldDeaths);
        model.addAttribute("lastExistedDay", worldDataHandler.getLastExistedDate());
        model.addAttribute("confirmedCases", worldDataHandler.getTodayConfirmedCases("World"));
        model.addAttribute("confirmedDeaths", worldDataHandler.getTodayConfirmedDeaths("World"));
        model.addAttribute("predictionDays", worldData.predictedWorldCases().size());
        model.addAttribute("predictedCases", worldDataHandler.getPredictedCases("World"));
        model.addAttribute("predictedDeaths", worldDataHandler.getPredictedDeaths("World"));
        model.addAttribute("lastDate", worldDataHandler.getLastDate());

        model.addAttribute("europeCases", worldDataHandler.getTodayConfirmedCases("Europe"));
        model.addAttribute("europeDeaths", worldDataHandler.getTodayConfirmedDeaths("Europe"));
        model.addAttribute("americaCases", worldDataHandler.getTodayConfirmedCases("Americas"));
        model.addAttribute("americaDeaths", worldDataHandler.getTodayConfirmedDeaths("Americas"));
        model.addAttribute("asiaCases", worldDataHandler.getTodayConfirmedCases("Asia"));
        model.addAttribute("asiaDeaths", worldDataHandler.getTodayConfirmedDeaths("Asia"));
        model.addAttribute("oceaniaCases", worldDataHandler.getTodayConfirmedCases("Oceania"));
        model.addAttribute("oceaniaDeaths", worldDataHandler.getTodayConfirmedDeaths("Oceania"));
        model.addAttribute("africaCases", worldDataHandler.getTodayConfirmedCases("Africa"));
        model.addAttribute("africaDeaths", worldDataHandler.getTodayConfirmedDeaths("Africa"));

        return "home";
    }

    @GetMapping("/changeWorld")
    @ResponseBody
    public List<TreeMap<String, Map<String, List<Integer>>>> changeWorldChartMode(@RequestParam("type") Boolean type,
                                                                                  @RequestParam("quantity") Boolean quantity) {
        WorldData worldData = fileReaderService.getWorldData();
        List<TreeMap<String, Map<String, List<Integer>>>> list = new ArrayList<>();
        if (type) {
            if (quantity) {
                list.add(worldData.worldConfirmedCasesWeakly());
                list.add(worldData.worldConfirmedDeathsWeakly());
            } else {
                list.add(worldData.worldConfirmedCases());
                list.add(worldData.worldConfirmedDeaths());
            }
        } else {
            if (quantity) {
                list.add(worldData.worldCasesWeekly());
                list.add(worldData.worldDeathsWeekly());
            } else {
                list.add(worldData.getWorldCases());
                list.add(worldData.getWorldDeaths());
            }
        }

        return list;
    }

    @GetMapping("/{name}")
    public String getCountryStatistics(Model model, @PathVariable("name") String country) {
        CountryData countryData = fileReaderService.getCountryData(country);
        CountryDataHandler countryDataHandler = new CountryDataHandler(countryData);
        model.addAttribute("country", country);
        model.addAttribute("cases", countryData.getCountryCases());
        model.addAttribute("deaths", countryData.getCountryDeaths());
        model.addAttribute("lastExistedDay", countryDataHandler.getLastExistedDate());
        model.addAttribute("confirmedCases", countryDataHandler.getTodayConfirmedCases());
        model.addAttribute("confirmedDeaths", countryDataHandler.getTodayConfirmedDeaths());
        model.addAttribute("predictionDays", countryData.predictedCountryCases().size());
        model.addAttribute("predictedCases", countryDataHandler.getPredictedCases());
        model.addAttribute("predictedDeaths", countryDataHandler.getPredictedDeaths());
        model.addAttribute("lastDate", countryDataHandler.getLastDate());

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

    @GetMapping("/{country}/chartMode")
    @ResponseBody
    public List<TreeMap<String, List<Integer>>> changeChartMode(@RequestParam("type") Boolean type,
                                                                @RequestParam("quantity") Boolean quantity,
                                                                @PathVariable("country") String country) {
        CountryData countryData = fileReaderService.getCountryData(country);
        List<TreeMap<String, List<Integer>>> list = new ArrayList<>();
        if (type) {
            if (quantity) {
                list.add(countryData.confirmedCasesWeakly());
                list.add(countryData.confirmedDeathsWeakly());
            } else {
                list.add(countryData.countryConfirmedCases());
                list.add(countryData.countryConfirmedDeaths());
            }
        } else {
            if (quantity) {
                list.add(countryData.countryCasesWeekly());
                list.add(countryData.countryDeathsWeekly());
            } else {
                list.add(countryData.getCountryCases());
                list.add(countryData.getCountryDeaths());
            }
        }

        return list;
    }

    @GetMapping("/csv/{country}")
    public void exportIntoCSV(HttpServletResponse response, @PathVariable("country") String country) throws IOException {
        response.setContentType("text/csv");
        response.addHeader("Content-Disposition", "attachment; filename=\"" + country + ".csv\"");
        fileReaderService.writeCasesToCsv(fileReaderService.getCountryData(country), country, response.getWriter());
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
