package com.sapiofan.predictions.services.impl;

import com.sapiofan.predictions.dao.CountryDao;
import com.sapiofan.predictions.dao.DateDao;
import com.sapiofan.predictions.entities.CountryData;
import com.sapiofan.predictions.entities.Data;
import com.sapiofan.predictions.entities.WorldData;
import com.sapiofan.predictions.entities.db.Country;
import com.sapiofan.predictions.entities.db.CovidDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DBHandlerService {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired
    private DateDao dateDao;

    @Autowired
    private CountryDao countryDao;

    public void updateExistedData(Data data) {
        if(data == null || data.getConfirmedCases().size() < 30) {
            return;
        }
        List<CovidDate> dates = dateDao.findAll();
        dates.sort(CovidDate::compareTo);
        List<Country> countries = new ArrayList<>();
        if(dates.size() < 30) {
            for (Map.Entry<String, Map<String, Long>> stringMapEntry : data.getConfirmedCases().entrySet()) {
                LocalDate date = LocalDate.parse(stringMapEntry.getKey()
                        .substring(stringMapEntry.getKey().indexOf(".")), formatter);
                CovidDate covidDate = dateDao.save(new CovidDate(date, false));
                for (Map.Entry<String, Long> stringLongEntry : stringMapEntry.getValue().entrySet()) {
                    countries.add(new Country(date.format(dbFormatter), data.getNewCases().get(stringMapEntry.getKey())
                            .get(stringLongEntry.getKey()),
                            data.getNewDeaths().get(stringMapEntry.getKey()).get(stringLongEntry.getKey()),
                            Math.toIntExact(data.getConfirmedCases().get(stringMapEntry.getKey()).get(stringLongEntry.getKey())),
                            data.getDeaths().get(stringMapEntry.getKey()).get(stringLongEntry.getKey()),
                            null, null, null, null,
                            covidDate));
                }
            }
            countryDao.saveAll(countries);
            return;
        }

        LocalDate localDate = LocalDate.now();

        for (int i = 0; i < dates.size(); i++) {
            if(dates.get(i).getPredictionDate()) {
                for (int j = 15; j > 0; j--) {
                    CovidDate covidDate = dates.get(i - j);
                    List<Country> countryList = countryDao.getCountriesByDate(covidDate);
                    for (Country country : countryList) {
                        country.setNew_cases(data.getNewCases().get(covidDate.getDate().format(formatter) + ".csv")
                                .get(country.getCountry()));
                        country.setNew_deaths(data.getNewDeaths().get(covidDate.getDate().format(formatter) + ".csv")
                                .get(country.getCountry()));
                        country.setConfirmed_cases(Math.toIntExact(data.getConfirmedCases().get(covidDate.getDate()
                                        .format(formatter) + ".csv").get(country.getCountry())));
                        country.setConfirmed_deaths(data.getDeaths().get(covidDate.getDate().format(formatter) + ".csv")
                                .get(country.getCountry()));
                    }
                    countryDao.saveAll(countryList);
                }
            }
            if(localDate.isAfter(dates.get(i).getDate())) {
                CovidDate covidDate = dates.get(i);
                List<Country> countryList = countryDao.getCountriesByDate(covidDate);
                for (Country country : countryList) {
                    country.setNew_cases(data.getNewCases().get(covidDate.getDate().format(formatter) + ".csv")
                            .get(country.getCountry()));
                    country.setNew_deaths(data.getNewDeaths().get(covidDate.getDate().format(formatter) + ".csv")
                            .get(country.getCountry()));
                    country.setConfirmed_cases(Math.toIntExact(data.getConfirmedCases().get(covidDate.getDate()
                            .format(formatter) + ".csv").get(country.getCountry())));
                    country.setConfirmed_deaths(data.getDeaths().get(covidDate.getDate().format(formatter) + ".csv")
                            .get(country.getCountry()));
                    country.setHigh_bound_cases(null);
                    country.setHigh_bound_deaths(null);
                    country.setLow_bound_cases(null);
                    country.setLow_bound_deaths(null);
                }
                countryDao.saveAll(countryList);
            } else {
                break;
            }
        }
        if(dates.get(dates.size() - 1).getDate().isBefore(localDate)) {
            for (Map.Entry<String, Map<String, Long>> stringMapEntry : data.getConfirmedCases().entrySet()) {
                LocalDate date = LocalDate.parse(stringMapEntry.getKey()
                        .substring(0, stringMapEntry.getKey().indexOf(".")), formatter);
                CovidDate covidDate = dateDao.save(new CovidDate(date, false));
                if(date.isBefore(localDate)) {
                    for (Map.Entry<String, Long> stringLongEntry : stringMapEntry.getValue().entrySet()) {
                        countries.add(new Country(date.format(dbFormatter), data.getNewCases().get(stringMapEntry.getKey())
                                .get(stringLongEntry.getKey()),
                                data.getNewDeaths().get(stringMapEntry.getKey()).get(stringLongEntry.getKey()),
                                Math.toIntExact(data.getConfirmedCases().get(stringMapEntry.getKey()).get(stringLongEntry.getKey())),
                                data.getDeaths().get(stringMapEntry.getKey()).get(stringLongEntry.getKey()),
                                null, null, null, null,
                                covidDate));
                    }
                }
            }
            countryDao.saveAll(countries);
        }
    }

    public void updatePredictedData(Data data) {
        if(data == null || data.getPredictionNewCases().size() < 30) {
            return;
        }
        List<CovidDate> dates = dateDao.findAll();
        dates.sort(CovidDate::compareTo);
        LocalDate localDate = LocalDate.now().minusDays(1);

        for (CovidDate date : dates) {
            if(date.getDate().isAfter(localDate)) {
                List<Country> countries = countryDao.getCountriesByDate(date);
                for (Country country : countries) {
                    List<Integer> predCases = data.getPredictionNewCases().get(date.getDate().format(formatter) + ".csv")
                            .get(country.getCountry());
                    List<Integer> predDeaths = data.getPredictionNewDeaths().get(date.getDate().format(formatter) + ".csv")
                            .get(country.getCountry());
                    country.setNew_cases(predCases.get(0));
                    country.setNew_deaths(predDeaths.get(0));
                    country.setConfirmed_cases(Math.toIntExact(data.getPredictionConfirmedCases().get(date.getDate()
                            .format(formatter) + ".csv").get(country.getCountry())));
                    country.setConfirmed_deaths(data.getPredictionConfirmedDeaths().get(date.getDate().format(formatter) + ".csv")
                            .get(country.getCountry()));
                    country.setHigh_bound_cases(predCases.get(2));
                    country.setHigh_bound_deaths(predDeaths.get(2));
                    country.setLow_bound_cases(predCases.get(1));
                    country.setLow_bound_deaths(predDeaths.get(1));
                }
            }
        }

        List<Country> countries = new ArrayList<>();

        for (Map.Entry<String, Map<String, List<Integer>>> stringMapEntry : data.getPredictionNewCases().entrySet()) {
            LocalDate predDate = LocalDate.parse(stringMapEntry.getKey()
                    .substring(stringMapEntry.getKey().indexOf(".")), formatter);
            CovidDate covidDate = dateDao.save(new CovidDate(predDate, false));
            if(dates.get(dates.size() - 1).getDate().isBefore(predDate)) {
                Map<String, List<Integer>> predDeaths = data.getPredictionNewDeaths().get(stringMapEntry.getKey());
                for (Map.Entry<String, List<Integer>> stringListEntry : stringMapEntry.getValue().entrySet()) {
                    countries.add(new Country(predDate.format(dbFormatter), stringListEntry.getValue().get(0),
                            predDeaths.get(stringListEntry.getKey()).get(0),
                            Math.toIntExact(data.getPredictionConfirmedCases().get(stringMapEntry.getKey())
                                    .get(stringListEntry.getKey())),
                            data.getPredictionConfirmedDeaths().get(stringMapEntry.getKey()).get(stringListEntry.getKey()),
                            stringListEntry.getValue().get(1), stringListEntry.getValue().get(2),
                            predDeaths.get(stringListEntry.getKey()).get(1), predDeaths.get(stringListEntry.getKey()).get(2),
                            covidDate));
                }
            }
        }
        countryDao.saveAll(countries);
    }

    public WorldData worldData(int period) {
        WorldData data = new WorldData();
        List<Country> regions = countryDao.getRegionsForPeriod(LocalDate.now().minusDays(period));

        return data;
    }

    public CountryData countryData(int period, String country) {
        CountryData data = new CountryData(country);
        List<Country> countryList = countryDao.getCountry(LocalDate.now().minusDays(period), country);

        return data;
    }
}
