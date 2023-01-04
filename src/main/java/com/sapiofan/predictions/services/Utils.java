package com.sapiofan.predictions.services;

import com.sapiofan.predictions.services.impl.FileHandlerServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class Utils {
    public static List<String> getCountries(FileHandlerService fileHandlerService) {
        return fileHandlerService.countriesFromFile();
    }
}
