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
        if (data == null || data.getConfirmedCases().size() < 30) {
            return;
        }
        List<CovidDate> dates = dateDao.findAll();
        dates.sort(CovidDate::compareTo);
        List<Country> countries = new ArrayList<>();
        if (dates.size() < 30) {
            TreeMap<String, Map<String, Long>> map = new TreeMap<>(data.dateComparator());
            map.putAll(data.getConfirmedCases());
            for (Map.Entry<String, Map<String, Long>> stringMapEntry : map.entrySet()) {
                if(map.firstKey().equals(stringMapEntry.getKey())) {
                    continue;
                }
                LocalDate date = LocalDate.parse(stringMapEntry.getKey()
                        .substring(0, stringMapEntry.getKey().indexOf(".")), formatter);
                Optional<CovidDate> covidDate = dates.stream()
                        .filter(covidDate1 -> covidDate1.getDate().equals(date)).findFirst();
                CovidDate covidDate1;
                if(covidDate.isEmpty()) {
                    covidDate1 = dateDao.save(new CovidDate(date, false));
                } else {
                    covidDate1 = covidDate.get();
                }
                for (Map.Entry<String, Long> stringLongEntry : stringMapEntry.getValue().entrySet()) {
//                    System.out.println(stringMapEntry.getKey());
//                    TreeMap<String, Map<String, Integer>> map1 = new TreeMap<>(data.dateComparator());
//                    map1.putAll(data.getNewCases());
//                    for (Map.Entry<String, Map<String, Integer>> mapEntry : map1.entrySet()) {
//                        System.out.println(mapEntry.getKey() + " : " + mapEntry.getValue());
//                    }
                    countries.add(new Country(stringLongEntry.getKey(), data.getNewCases().get(stringMapEntry.getKey())
                            .get(stringLongEntry.getKey()),
                            data.getNewDeaths().get(stringMapEntry.getKey()).get(stringLongEntry.getKey()),
                            Math.toIntExact(data.getConfirmedCases().get(stringMapEntry.getKey()).get(stringLongEntry.getKey())),
                            data.getDeaths().get(stringMapEntry.getKey()).get(stringLongEntry.getKey()),
                            null, null, null, null,
                            covidDate1));
                }
            }
            countryDao.saveAll(countries);
            return;
        }

        LocalDate localDate = LocalDate.now();

        for (int i = 0; i < dates.size(); i++) {
            if (dates.get(i).getPredictionDate()) {
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
            if (localDate.isAfter(dates.get(i).getDate())) {
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
        if (dates.get(dates.size() - 1).getDate().isBefore(localDate)) {
            TreeMap<String, Map<String, Long>> map = new TreeMap<>(data.dateComparator());
            map.putAll(data.getConfirmedCases());
            for (Map.Entry<String, Map<String, Long>> stringMapEntry : map.entrySet()) {
                if(map.firstKey().equals(stringMapEntry.getKey())) {
                    continue;
                }
                LocalDate date = LocalDate.parse(stringMapEntry.getKey()
                        .substring(0, stringMapEntry.getKey().indexOf(".")), formatter);
                Optional<CovidDate> covidDate = dates.stream()
                        .filter(covidDate1 -> covidDate1.getDate().equals(date)).findFirst();
                CovidDate covidDate1;
                if(covidDate.isEmpty()) {
                    covidDate1 = dateDao.save(new CovidDate(date, false));
                } else {
                    covidDate1 = covidDate.get();
                }
                if (date.isBefore(localDate)) {
                    for (Map.Entry<String, Long> stringLongEntry : stringMapEntry.getValue().entrySet()) {
                        countries.add(new Country(stringLongEntry.getKey(), data.getNewCases().get(stringMapEntry.getKey())
                                .get(stringLongEntry.getKey()),
                                data.getNewDeaths().get(stringMapEntry.getKey()).get(stringLongEntry.getKey()),
                                Math.toIntExact(data.getConfirmedCases().get(stringMapEntry.getKey()).get(stringLongEntry.getKey())),
                                data.getDeaths().get(stringMapEntry.getKey()).get(stringLongEntry.getKey()),
                                null, null, null, null,
                                covidDate1));
                    }
                }
            }
            countryDao.saveAll(countries);
        }
    }

    public void updatePredictedData(Data data) {
        if (data == null || data.getPredictionNewCases().size() < 30) {
            return;
        }
        List<CovidDate> dates = dateDao.findAll();
        dates.sort(CovidDate::compareTo);
        LocalDate localDate = LocalDate.now().minusDays(1);

        for (CovidDate date : dates) {
            if (date.getDate().isAfter(localDate)) {
                date.setPredictionDate(true);
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
                countryDao.saveAll(countries);
            }
        }

        List<Country> countries = new ArrayList<>();

        TreeMap<String, Map<String, List<Integer>>> map = new TreeMap<>(data.dateComparator());
        map.putAll(data.getPredictionNewCases());
        for (Map.Entry<String, Map<String, List<Integer>>> stringMapEntry : map.entrySet()) {
            LocalDate predDate = LocalDate.parse(stringMapEntry.getKey()
                    .substring(0, stringMapEntry.getKey().indexOf(".")), formatter);
//            CovidDate covidDate = dateDao.findCovidDateByDate(predDate);
//            if(covidDate == null) {
//                covidDate = dateDao.save(new CovidDate(predDate, true));
//            }
            Optional<CovidDate> covidDate = dates.stream()
                    .filter(covidDate1 -> covidDate1.getDate().equals(predDate)).findFirst();
            CovidDate covidDate1;
            if(covidDate.isEmpty()) {
                covidDate1 = dateDao.save(new CovidDate(predDate, false));
            } else {
                covidDate1 = covidDate.get();
            }
            if (dates.get(dates.size() - 1).getDate().isBefore(predDate)) {
                Map<String, List<Integer>> predDeaths = data.getPredictionNewDeaths().get(stringMapEntry.getKey());
                for (Map.Entry<String, List<Integer>> stringListEntry : stringMapEntry.getValue().entrySet()) {
                    countries.add(new Country(stringListEntry.getKey(), stringListEntry.getValue().get(0),
                            predDeaths.get(stringListEntry.getKey()).get(0),
                            Math.toIntExact(data.getPredictionConfirmedCases().get(stringMapEntry.getKey())
                                    .get(stringListEntry.getKey())),
                            data.getPredictionConfirmedDeaths().get(stringMapEntry.getKey()).get(stringListEntry.getKey()),
                            stringListEntry.getValue().get(1), stringListEntry.getValue().get(2),
                            predDeaths.get(stringListEntry.getKey()).get(1), predDeaths.get(stringListEntry.getKey()).get(2),
                            covidDate1));
                }
            }
        }
        countryDao.saveAll(countries);
    }

    public WorldData worldData(int period) {
        WorldData data = new WorldData();
        List<Country> regions = countryDao.getRegionsForPeriod(LocalDate.now().minusDays(period));
        regions.sort(Country::compareTo);
        for (Country region : regions) {
            Map<String, List<Integer>> worldCases = data.getWorldCases().get(region.getDate().getDate().format(dbFormatter));
            Map<String, List<Integer>> worldDeaths = data.getWorldDeaths().get(region.getDate().getDate().format(dbFormatter));
            Map<String, List<Integer>> worldConfirmedCases = data.getConfirmedCases()
                    .get(region.getDate().getDate().format(dbFormatter));
            Map<String, List<Integer>> worldConfirmedDeaths = data.getConfirmedDeaths()
                    .get(region.getDate().getDate().format(dbFormatter));
            if (worldCases == null) {
                Map<String, List<Integer>> map = new HashMap<>();
                map.put(region.getCountry(), new ArrayList<>(Arrays.asList(region.getNew_cases(), region.getLow_bound_cases(),
                        region.getHigh_bound_cases())));
                data.getWorldCases().put(region.getDate().getDate().format(dbFormatter), map);
            } else {
                worldCases.put(region.getCountry(), new ArrayList<>(Arrays.asList(region.getNew_cases(), region.getLow_bound_cases(),
                        region.getHigh_bound_cases())));
            }
            if(worldDeaths == null) {
                Map<String, List<Integer>> map = new HashMap<>();
                map.put(region.getCountry(), new ArrayList<>(Arrays.asList(region.getNew_deaths(), region.getLow_bound_deaths(),
                        region.getHigh_bound_deaths())));
                data.getWorldDeaths().put(region.getDate().getDate().format(dbFormatter), map);
            } else {
                worldDeaths.put(region.getCountry(), new ArrayList<>(Arrays.asList(region.getNew_cases(), region.getLow_bound_cases(),
                        region.getHigh_bound_cases())));
            }
            if(worldConfirmedCases == null) {
                Map<String, List<Integer>> map = new HashMap<>();
                map.put(region.getCountry(), new ArrayList<>(List.of(region.getConfirmed_cases())));
                data.getConfirmedCases().put(region.getDate().getDate().format(dbFormatter), map);
            } else {
                worldConfirmedCases.put(region.getCountry(), new ArrayList<>(List.of(region.getConfirmed_cases())));
            }
            if(worldConfirmedDeaths == null) {
                Map<String, List<Integer>> map = new HashMap<>();
                map.put(region.getCountry(), new ArrayList<>(List.of(region.getConfirmed_deaths())));
                data.getConfirmedDeaths().put(region.getDate().getDate().format(dbFormatter), map);
            } else {
                worldConfirmedDeaths.put(region.getCountry(), new ArrayList<>(List.of(region.getConfirmed_deaths())));
            }
        }
        return data;
    }

    public CountryData countryData(int period, String country) {
        CountryData data = new CountryData(country);
        List<Country> countryList = countryDao.getCountry(LocalDate.now().minusDays(period), country);
        countryList.sort(Country::compareTo);

        TreeMap<String, List<Integer>> newCases = new TreeMap<>(data.dateComparator());
        TreeMap<String, List<Integer>> newDeaths = new TreeMap<>(data.dateComparator());
        TreeMap<String, List<Integer>> confirmedCases = new TreeMap<>(data.dateComparator());
        TreeMap<String, List<Integer>> confirmedDeaths = new TreeMap<>(data.dateComparator());
        for (Country country1 : countryList) {
            newCases.put(country1.getDate().getDate().format(dbFormatter), List.of(country1.getNew_cases(),
                    country1.getLow_bound_cases(), country1.getHigh_bound_cases()));
            newDeaths.put(country1.getDate().getDate().format(dbFormatter), List.of(country1.getNew_deaths(),
                    country1.getLow_bound_deaths(), country1.getLow_bound_cases()));
            confirmedCases.put(country1.getDate().getDate().format(dbFormatter), List.of(country1.getConfirmed_cases()));
            confirmedDeaths.put(country1.getDate().getDate().format(dbFormatter), List.of(country1.getConfirmed_deaths()));
        }
        data.setCountryCases(newCases);
        data.setCountryDeaths(newDeaths);
        data.setCountryConfirmedCases(confirmedCases);
        data.setCountryConfirmedDeaths(confirmedDeaths);

        return data;
    }
}
