package com.sapiofan.predictions.services;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    @Autowired
    private static FileHandlerService fileHandlerService;

    public static List<String> getCountries() {
        return fileHandlerService.countriesFromFile();
    }
}
