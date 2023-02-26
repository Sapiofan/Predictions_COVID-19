package com.sapiofan.predictions.services;

import com.sapiofan.predictions.entities.AllCountries;
import com.sapiofan.predictions.entities.CountryData;
import com.sapiofan.predictions.entities.WorldData;

import java.io.Writer;

public interface FileReaderService {
    WorldData getWorldData();

    CountryData getCountryData(String country);

    AllCountries getAllCountries();

    void writeCasesToCsv(CountryData data, String country, Writer writer);
}
