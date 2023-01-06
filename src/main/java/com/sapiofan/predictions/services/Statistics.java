package com.sapiofan.predictions.services;

import com.sapiofan.predictions.entities.Data;

public interface Statistics {
    Data getWorldData();
    void getWorldStatistics(Data data);
    void getCountryDataExponential(Data data, String country);
    void getCountryDataLinear(Data data);
}
