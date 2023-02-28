package com.sapiofan.predictions.services.impl;

import com.sapiofan.predictions.dao.CountryDao;
import com.sapiofan.predictions.dao.DateDao;
import com.sapiofan.predictions.entities.AllCountries;
import com.sapiofan.predictions.entities.CountryData;
import com.sapiofan.predictions.entities.Data;
import com.sapiofan.predictions.entities.WorldData;
import com.sapiofan.predictions.entities.db.Country;
import com.sapiofan.predictions.entities.db.CovidDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class DBHandlerService {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final Logger log = LoggerFactory.getLogger(DBHandlerService.class);

    private ReentrantLock locker = new ReentrantLock();

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
                if (map.firstKey().equals(stringMapEntry.getKey())) {
                    continue;
                }
                LocalDate date = LocalDate.parse(stringMapEntry.getKey()
                        .substring(0, stringMapEntry.getKey().indexOf(".")), formatter);
                Optional<CovidDate> covidDate = dates.stream()
                        .filter(covidDate1 -> covidDate1.getDate().equals(date)).findFirst();
                CovidDate covidDate1;
                if (covidDate.isEmpty()) {
                    covidDate1 = dateDao.save(new CovidDate(date, false));
                } else {
                    covidDate1 = covidDate.get();
                }
                stringMapEntry.getValue()
                        .forEach((key, value) -> countries
                                .add(new Country(key, data.getNewCases().get(stringMapEntry.getKey())
                                        .get(key),
                                        data.getNewDeaths().get(stringMapEntry.getKey()).get(key),
                                        Math.toIntExact(data.getConfirmedCases().get(stringMapEntry.getKey()).get(key)),
                                        data.getDeaths().get(stringMapEntry.getKey()).get(key),
                                        null, null, null, null,
                                        covidDate1)));
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
                if (map.firstKey().equals(stringMapEntry.getKey())) {
                    continue;
                }
                LocalDate date = LocalDate.parse(stringMapEntry.getKey()
                        .substring(0, stringMapEntry.getKey().indexOf(".")), formatter);
                Optional<CovidDate> covidDate = dates.stream()
                        .filter(covidDate1 -> covidDate1.getDate().equals(date)).findFirst();
                CovidDate covidDate1;
                if (covidDate.isEmpty()) {
                    covidDate1 = dateDao.save(new CovidDate(date, false));
                } else {
                    covidDate1 = covidDate.get();
                }
                if (date.isBefore(localDate)) {
                    stringMapEntry.getValue().forEach((key, value) -> countries
                            .add(new Country(key, data.getNewCases().get(stringMapEntry.getKey())
                                    .get(key),
                                    data.getNewDeaths().get(stringMapEntry.getKey()).get(key),
                                    Math.toIntExact(data.getConfirmedCases().get(stringMapEntry.getKey()).get(key)),
                                    data.getDeaths().get(stringMapEntry.getKey()).get(key),
                                    null, null, null, null,
                                    covidDate1)));
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
            Optional<CovidDate> covidDate = dates.stream()
                    .filter(covidDate1 -> covidDate1.getDate().equals(predDate)).findFirst();
            CovidDate covidDate1;
            if (covidDate.isEmpty()) {
                covidDate1 = dateDao.save(new CovidDate(predDate, false));
            } else {
                covidDate1 = covidDate.get();
            }
            if (dates.get(dates.size() - 1).getDate().isBefore(predDate)) {
                Map<String, List<Integer>> predDeaths = data.getPredictionNewDeaths().get(stringMapEntry.getKey());
                stringMapEntry.getValue().forEach((key, value) -> countries.add(new Country(key, value.get(0),
                        predDeaths.get(key).get(0),
                        Math.toIntExact(data.getPredictionConfirmedCases().get(stringMapEntry.getKey())
                                .get(key)),
                        data.getPredictionConfirmedDeaths().get(stringMapEntry.getKey()).get(key),
                        value.get(1), value.get(2),
                        predDeaths.get(key).get(1), predDeaths.get(key).get(2),
                        covidDate1)));
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
            if (worldDeaths == null) {
                Map<String, List<Integer>> map = new HashMap<>();
                map.put(region.getCountry(), new ArrayList<>(Arrays.asList(region.getNew_deaths(), region.getLow_bound_deaths(),
                        region.getHigh_bound_deaths())));
                data.getWorldDeaths().put(region.getDate().getDate().format(dbFormatter), map);
            } else {
                worldDeaths.put(region.getCountry(), new ArrayList<>(Arrays.asList(region.getNew_cases(), region.getLow_bound_cases(),
                        region.getHigh_bound_cases())));
            }
            if (worldConfirmedCases == null) {
                Map<String, List<Integer>> map = new HashMap<>();
                map.put(region.getCountry(), new ArrayList<>(List.of(region.getConfirmed_cases())));
                data.getConfirmedCases().put(region.getDate().getDate().format(dbFormatter), map);
            } else {
                worldConfirmedCases.put(region.getCountry(), new ArrayList<>(List.of(region.getConfirmed_cases())));
            }
            if (worldConfirmedDeaths == null) {
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

    public AllCountries allCountries() {
        AllCountries allCountries = new AllCountries();
        List<Country> countries = countryDao.findAll();
        List<CovidDate> dates = dateDao.findAll();
        for (CovidDate date : dates) {
            allCountries.getNewCases().put(date.getDate().format(dbFormatter), new HashMap<>());
            allCountries.getNewDeaths().put(date.getDate().format(dbFormatter), new HashMap<>());
            allCountries.getConfirmedCases().put(date.getDate().format(dbFormatter), new HashMap<>());
            allCountries.getConfirmedDeaths().put(date.getDate().format(dbFormatter), new HashMap<>());
        }
        final int chunk = countries.size() / 12;
        Thread startChunk = new Thread(() -> parallelReading(allCountries, countries.subList(0, chunk)));
        startChunk.start();
        for (int i = 2; i < 12; i++) {
            int finalI = i;
            Thread midChunk = new Thread(() -> parallelReading(allCountries, countries.subList(chunk * (finalI - 1), chunk * finalI)));
            midChunk.start();
        }
        Thread endChunk = new Thread(() -> parallelReading(allCountries, countries.subList(chunk * 11, countries.size())));
        endChunk.start();
        return allCountries;
    }

    private void parallelReading(AllCountries allCountries, List<Country> countries) {
        for (Country country : countries) {
            if (!country.getCountry().equals("World") && !country.getCountry().equals("Europe") &&
                    !country.getCountry().equals("Asia") && !country.getCountry().equals("Americas") &&
                    !country.getCountry().equals("Oceania") && !country.getCountry().equals("Africa")) {
                allCountries.getNewCases().get(country.getDate().getDate().format(dbFormatter))
                        .put(country.getCountry(), country.getNew_cases());
                allCountries.getNewDeaths().get(country.getDate().getDate().format(dbFormatter))
                        .put(country.getCountry(), country.getNew_cases());
                allCountries.getConfirmedCases().get(country.getDate().getDate().format(dbFormatter))
                        .put(country.getCountry(), Long.valueOf(country.getConfirmed_cases()));
                allCountries.getConfirmedDeaths().get(country.getDate().getDate().format(dbFormatter))
                        .put(country.getCountry(), country.getNew_cases());
            }
        }
    }
}
