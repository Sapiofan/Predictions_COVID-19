package com.sapiofan.predictions.services.impl;

import com.sapiofan.predictions.services.FileHandlerService;

import java.util.List;

public class Utils {
    public static List<String> getCountries(FileHandlerService fileHandlerService) {
        return fileHandlerService.countriesFromFile();
    }
}
