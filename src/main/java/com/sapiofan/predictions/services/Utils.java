package com.sapiofan.predictions.services;

import java.util.List;

public class Utils {
    public static List<String> getCountries(FileHandlerService fileHandlerService) {
        return fileHandlerService.countriesFromFile();
    }
}
