package com.sapiofan.predictions.services;

import com.sapiofan.predictions.entities.Data;

import java.util.List;
import java.util.Map;

public interface FileHandlerService {
    void downloadFilesWithData(int days);

    void readData(Data data);

    void writeToCSV(Data data);

    List<String> countriesFromFile();

    Map<String, String> countriesByRegions();

    void downloadFileFromOurWorldData();

    void readDataFromOneFile(Data data, int days);
}
