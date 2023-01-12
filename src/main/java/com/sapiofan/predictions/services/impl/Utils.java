package com.sapiofan.predictions.services.impl;

import com.sapiofan.predictions.services.FileHandlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class Utils {

    @Autowired
    @Lazy
    private FileHandlerService fileHandlerService;

    public List<String> getCountries() {
        return fileHandlerService.countriesFromFile();
    }

    public Map<String, String> getCountriesByRegions() {
        return fileHandlerService.countriesByRegions();
    }

    public boolean compareDateAndString(String sDate, int days, DateTimeFormatter formatter) {
        LocalDate day = LocalDate.parse(sDate.substring(0, sDate.indexOf(".")), formatter);
        LocalDate lastDate = LocalDate.now().minusDays(days);

        return lastDate.isEqual(day);
    }
}
