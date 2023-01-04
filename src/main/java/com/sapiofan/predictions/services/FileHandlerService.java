package com.sapiofan.predictions.services;

import com.sapiofan.predictions.entities.Data;

import java.io.File;
import java.util.List;

public interface FileHandlerService {
    void downloadFilesWithData();
    void readData(Data data);
    void writeToCSV(Data data);
    List<String> countriesFromFile();
}
