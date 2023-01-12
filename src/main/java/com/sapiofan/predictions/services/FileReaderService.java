package com.sapiofan.predictions.services;

import com.sapiofan.predictions.entities.CountryData;
import com.sapiofan.predictions.entities.WorldData;

public interface FileReaderService {
    WorldData getWorldData();

    CountryData getCountryData(String country);
}
