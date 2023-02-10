package com.sapiofan.predictions.entities;

public class CountryDataHandler {

    private final CountryData countryData;

    public CountryDataHandler(CountryData countryData) {
        this.countryData = countryData;
    }

    public Integer getTodayConfirmedCases() {
        return countryData.existedCountryConfirmedCases().lastEntry().getValue().get(0);
    }

    public Integer getTodayConfirmedDeaths() {
        return countryData.existedCountryConfirmedDeaths().lastEntry().getValue().get(0);
    }

    public Integer getPredictedCases() {
        return countryData.predictedCountryConfirmedCases().lastEntry().getValue().get(0);
    }

    public Integer getPredictedDeaths() {
        return countryData.predictedCountryConfirmedDeaths().lastEntry().getValue().get(0);
    }

    public String getLastExistedDate() {
        return countryData.existedCountryCases().lastEntry().getKey();
    }

    public String getLastDate() {
        return countryData.predictedCountryCases().lastEntry().getKey();
    }
}
