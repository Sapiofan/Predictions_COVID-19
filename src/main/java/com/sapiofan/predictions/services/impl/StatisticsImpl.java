package com.sapiofan.predictions.services.impl;

import com.sapiofan.predictions.entities.Data;
import com.sapiofan.predictions.services.FileHandlerService;
import com.sapiofan.predictions.services.Statistics;
import com.sapiofan.predictions.services.regression.ExponentialSmoothing;
import com.sapiofan.predictions.services.regression.GompertzGrowth;
import com.sapiofan.predictions.services.regression.LinearRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class StatisticsImpl implements Statistics {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private static final Logger log = LoggerFactory.getLogger(StatisticsImpl.class);

    private final int DAYS = 180;

    private static final String WORLD = "World";
    private static final String CSV_EXTENSION = ".csv";

    @Autowired
    private FileHandlerService fileHandlerService;

    @Autowired
    private Utils utils;

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Data data = getWorldData();
                TreeMap<String, Integer> map = new TreeMap<>(data.dateComparator());
                for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getDeaths().entrySet()) {
                    for (Map.Entry<String, Integer> stringIntegerEntry : stringMapEntry.getValue().entrySet()) {
                        if(stringIntegerEntry.getKey().equals("Italy")) {
                            map.put(stringMapEntry.getKey(), stringIntegerEntry.getValue());
                        }
                    }
                }
                TreeMap<String, Integer> map2 = new TreeMap<>(data.dateComparator());
                int counter = 0;
                for (Map.Entry<String, Integer> stringIntegerEntry : map.entrySet()) {
                    if(counter > map.size() - 30) {
                        map2.put(stringIntegerEntry.getKey(), stringIntegerEntry.getValue());
                    }
                    counter++;
                }
                GompertzGrowth gompertzGrowth = new GompertzGrowth(map2, data);
                gompertzGrowth.predictionCases();
//                ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(12);
//
//                log.warn("Started calculating exponential smooth");
//                Date start = new Date();
//
//                for (String country : utils.getCountries()) {
//                    executor.execute(() -> getCountryDataExponential(data, country));
//                }
//
//                Set<String> areas = new HashSet<>(utils.getCountriesByRegions().values());
//                for (String area : areas) {
//                    executor.execute(() -> getCountryDataExponential(data, area));
//                }
//                executor.execute(() -> getCountryDataExponential(data, WORLD));
//
//                while (true) {
//                    if (executor.getActiveCount() == 0) {
//                        handlePredictedCases(data);
//                        Date end = new Date();
//                        log.warn("Ended calculating exponential smooth. Time: " + (end.getTime() - start.getTime()) / 1000);
//                        log.warn("Start writing to csv");
//                        fileHandlerService.writeToCSV(data);
//                        log.warn("End writing to csv");
//                        break;
//                    } else {
//                        try {
//                            Thread.sleep(3000);
//                        } catch (InterruptedException e) {
//                            log.error("Can't sleep timer: " + e);
//                        }
//                    }
//                }
//                getCountryDataLinear(data);
            }
        }, 0, 86400000);
    }

    @Override
    public Data getWorldData() {
        log.warn("Started downloading of csv files from the site");
//        fileHandlerService.downloadFilesWithData(DAYS);
        log.warn("Ended downloading of csv files from the site");
        Data data = new Data();
        fileHandlerService.readData(data);
        calculateNewCases(data);
        calculateNewDeaths(data);
        addRegionsToConfirmedData(data);

        return data;
    }

    private void handlePredictedCases(Data data) {
        calculatePredictedConfirmedCases(data);
        calculatePredictedConfirmedDeaths(data);
    }

    @Override
    public void getWorldStatistics(Data data) {
        getCountryDataExponential(data, WORLD);
    }

    @Override
    public void getCountryDataExponential(Data data, String country) {
        ExponentialSmoothing exponentialSmoothing = new ExponentialSmoothing();
        exponentialSmoothing.predictionCases(data, getNewCasesOfCountry(data, country), country);
        exponentialSmoothing.predictionDeaths(data, getNewDeathsOfCountry(data, country), country);
    }

    @Override
    public void getCountryDataLinear(Data data) {
        LinearRegression linearRegression = new LinearRegression();
        analyzeNewCasesForWorld(data, linearRegression);
        analyzeNewDeathsForWorld(data, linearRegression);
        data.getNewCases().entrySet().stream().findFirst()
                .ifPresent(stringMapEntry -> stringMapEntry.getValue().forEach((key, value) ->
                        analyzeNewCasesForCountry(data, key, linearRegression)));
        data.getNewDeaths().entrySet().stream().findFirst()
                .ifPresent(stringMapEntry -> stringMapEntry.getValue().forEach((key, value) ->
                        analyzeNewDeathsForCountry(data, key, linearRegression)));
    }

    private void calculateNewCases(Data data) {
        Map<String, Map<String, Integer>> newCases = new HashMap<>();

        boolean flag = false;

        for (Map.Entry<String, Map<String, Long>> stringMapEntry : data.getConfirmedCases().entrySet()) {

            Map<String, Long> previousDay = data.getConfirmedCases().get(LocalDate.parse(stringMapEntry.getKey()
                            .substring(0, stringMapEntry.getKey().lastIndexOf(".")), formatter)
                    .minusDays(1L).format(formatter) + CSV_EXTENSION);

            Map<String, Integer> newCasesDay = new HashMap<>();
            for (Map.Entry<String, Long> stringIntegerEntry : stringMapEntry.getValue().entrySet()) {
                if (previousDay == null) {
                    flag = true;
                    break;
                }
                newCasesDay.put(stringIntegerEntry.getKey(),
                        (int) Math.max(stringIntegerEntry.getValue() - previousDay.get(stringIntegerEntry.getKey()), 0));
            }
            if (flag) {
                flag = false;
                continue;
            }
            newCases.put(stringMapEntry.getKey(), newCasesDay);
        }

        data.setNewCases(newCases);

        Map<String, Integer> worldCases = getWorldCases(data);

        data.getNewCases().forEach((key, value) -> value.put(WORLD, worldCases.get(key)));

        Map<String, String> countries = utils.getCountriesByRegions();
        Set<String> regions = new HashSet<>(countries.values());

        regions.forEach(region -> data.getNewCases()
                .forEach((key, value) -> value.put(region, getRegionCases(data, countries.entrySet()
                        .stream()
                        .filter(stringStringEntry -> stringStringEntry.getValue().equals(region))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList()))
                        .get(key))));
    }

    private void calculateNewDeaths(Data data) {
        Map<String, Map<String, Integer>> newDeaths = new HashMap<>();
        boolean flag = false;

        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getDeaths().entrySet()) {

            Map<String, Integer> previousDay = data.getDeaths().get(LocalDate.parse(stringMapEntry.getKey()
                            .substring(0, stringMapEntry.getKey().lastIndexOf(".")), formatter)
                    .minusDays(1L).format(formatter) + CSV_EXTENSION);


            Map<String, Integer> newDeathsDay = new HashMap<>();
            for (Map.Entry<String, Integer> stringIntegerEntry : stringMapEntry.getValue().entrySet()) {
                if (previousDay == null) {
                    flag = true;
                    break;
                }
                newDeathsDay.put(stringIntegerEntry.getKey(),
                        Math.max(stringIntegerEntry.getValue() - previousDay.get(stringIntegerEntry.getKey()), 0));
            }
            if (flag) {
                flag = false;
                continue;
            }

            newDeaths.put(stringMapEntry.getKey(), newDeathsDay);
        }

        data.setNewDeaths(newDeaths);
        Map<String, Integer> worldDeaths = getWorldDeaths(data);

        data.getNewDeaths()
                .forEach((key, value) -> worldDeaths.entrySet()
                        .stream()
                        .filter(stringIntegerEntry -> stringIntegerEntry.getKey().equals(key))
                        .findFirst()
                        .ifPresent(stringIntegerEntry -> value.put(WORLD, stringIntegerEntry.getValue())));

        Map<String, String> countries = utils.getCountriesByRegions();
        Set<String> regions = new HashSet<>(countries.values());

        regions.forEach(region -> data.getNewDeaths()
                .forEach((key, value) -> value.put(region, getRegionDeaths(data, countries.entrySet()
                        .stream()
                        .filter(stringStringEntry -> stringStringEntry.getValue().equals(region))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList()))
                        .get(key))));
    }

    private void addRegionsToConfirmedData(Data data) {
        Map<String, String> countries = utils.getCountriesByRegions();
        Set<String> regions = new HashSet<>(countries.values());
        for (String region : regions) {
            calculateConfirmedCasesForRegion(region, countries, data);
            calculateConfirmedDeathsForRegion(region, countries, data);
        }
        calculateConfirmedCasesForWorld(data, regions);
        calculateConfirmedDeathsForWorld(data, regions);
    }

    private void calculateConfirmedCasesForWorld(Data data, Set<String> regions) {
        long sum = 0;
        for (Map.Entry<String, Map<String, Long>> stringMapEntry : data.getConfirmedCases().entrySet()) {
            sum += stringMapEntry.getValue().entrySet()
                    .stream()
                    .filter(stringLongEntry -> regions.contains(stringLongEntry.getKey()))
                    .mapToLong(Map.Entry::getValue)
                    .sum();
            stringMapEntry.getValue().put(WORLD, sum);
            sum = 0;
        }
    }

    private void calculateConfirmedDeathsForWorld(Data data, Set<String> regions) {
        int sum = 0;
        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getDeaths().entrySet()) {
            sum += stringMapEntry.getValue().entrySet()
                    .stream()
                    .filter(stringLongEntry -> regions.contains(stringLongEntry.getKey()))
                    .mapToInt(Map.Entry::getValue)
                    .sum();
            stringMapEntry.getValue().put(WORLD, sum);
            sum = 0;
        }
    }

    private void calculateConfirmedCasesForRegion(String region, Map<String, String> countries, Data data) {
        long sum = 0;
        for (Map.Entry<String, Map<String, Long>> stringMapEntry : data.getConfirmedCases().entrySet()) {
            sum += stringMapEntry.getValue().entrySet()
                    .stream()
                    .filter(stringIntegerEntry -> countries.get(stringIntegerEntry.getKey()) != null
                    && countries.get(stringIntegerEntry.getKey()).equals(region))
                    .mapToLong(Map.Entry::getValue)
                    .sum();
            stringMapEntry.getValue().put(region, sum);
            sum = 0;
        }
    }

    private void calculateConfirmedDeathsForRegion(String region, Map<String, String> countries, Data data) {
        int sum = 0;
        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getDeaths().entrySet()) {
            sum += stringMapEntry.getValue().entrySet()
                    .stream()
                    .filter(stringIntegerEntry -> countries.get(stringIntegerEntry.getKey()) != null
                    && countries.get(stringIntegerEntry.getKey()).equals(region))
                    .mapToInt(Map.Entry::getValue)
                    .sum();
            stringMapEntry.getValue().put(region, sum);
            sum = 0;
        }
    }

    private void analyzeNewCasesForWorld(Data data, LinearRegression regression) {
        Map<String, Integer> worldCases = getNewCasesOfCountry(data, WORLD);
        List<Double> betterLine = analyzeData(data, worldCases, regression);

        for (int i = 1; i <= DAYS; i++) {
            Map<String, List<Integer>> worldPrediction = new HashMap<>();
            List<Integer> list = new ArrayList<>();
            list.add((int) regression.getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            worldPrediction.put(WORLD, list);
            data.getPredictionNewCases().put(LocalDate.now().plusDays(i - 1).format(formatter) + CSV_EXTENSION, worldPrediction);
        }
    }

    private void analyzeNewDeathsForWorld(Data data, LinearRegression regression) {
        Map<String, Integer> worldCases = getWorldDeaths(data);
        List<Double> betterLine = analyzeData(data, worldCases, regression);

        for (int i = 1; i <= DAYS; i++) {
            Map<String, List<Integer>> worldPrediction = new HashMap<>();
            List<Integer> list = new ArrayList<>();
            list.add((int) regression.getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            worldPrediction.put(WORLD, list);
            data.getPredictionNewDeaths().put(LocalDate.now().plusDays(i - 1).format(formatter) + CSV_EXTENSION, worldPrediction);
        }
    }

    private void analyzeNewCasesForCountry(Data data, String country, LinearRegression regression) {
        Map<String, Integer> countryCases = getNewCasesOfCountry(data, country);
        List<Double> betterLine = analyzeData(data, countryCases, regression);

        for (int i = 1; i <= DAYS; i++) {
            String date = LocalDate.now().plusDays(i - 1).format(formatter) + CSV_EXTENSION;
            Map<String, List<Integer>> countryPrediction = data.getPredictionNewCases().entrySet()
                    .stream().filter(s -> s.getKey().equals(date))
                    .findFirst().map(Map.Entry::getValue).orElse(new HashMap<>());
            List<Integer> list = new ArrayList<>();
            list.add((int) regression
                    .getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            countryPrediction.put(country, list);
            data.getPredictionNewCases().replace(date, countryPrediction);
        }
    }

    private void analyzeNewDeathsForCountry(Data data, String country, LinearRegression regression) {
        Map<String, Integer> countryDeaths = getNewDeathsOfCountry(data, country);
        List<Double> betterLine = analyzeData(data, countryDeaths, regression);

        for (int i = 1; i <= DAYS; i++) {
            String date = LocalDate.now().plusDays(i - 1).format(formatter) + CSV_EXTENSION;
            Map<String, List<Integer>> countryPrediction = data.getPredictionNewDeaths()
                    .entrySet().stream().filter(s -> s.getKey().equals(date))
                    .findFirst().map(Map.Entry::getValue).orElse(new HashMap<>());
            List<Integer> list = new ArrayList<>();
            list.add((int) regression
                    .getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            countryPrediction.put(country, list);
            data.getPredictionNewDeaths().replace(date, countryPrediction);
        }
    }

    private List<Double> analyzeData(Data data, Map<String, Integer> countryCases, LinearRegression regression) {
        List<Double> simpleRegression = regression.SimpleRegression(data, countryCases);
        double simpleRegressionErr = regression.MSE(data, countryCases, simpleRegression);
        List<Double> descendentGradient = regression.gradientDescent(data, countryCases);
        double descendentGradientErr = regression.MSE(data, countryCases, descendentGradient);

//        log.warn("Regression: " + simpleRegressionErr);
//        log.warn("Gradient result: " + descendentGradientErr);

        List<Double> betterLine;

        if (simpleRegressionErr > descendentGradientErr) {
            betterLine = descendentGradient;
        } else {
            betterLine = simpleRegression;
        }

        return betterLine;
    }

    private Map<String, Integer> getNewCasesOfCountry(Data data, String country) {
        return data.getNewCases().entrySet().stream().findFirst().get().getValue().get(country) == null ? null :
                data.getNewCases().entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, stringMapEntry ->
                                stringMapEntry.getValue().get(country), (a, b) -> b));
    }

    private Map<String, Integer> getNewDeathsOfCountry(Data data, String country) {
        return data.getNewDeaths().entrySet().stream().findFirst().get().getValue().get(country) == null ? null :
                data.getNewDeaths().entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, stringMapEntry ->
                                stringMapEntry.getValue().get(country), (a, b) -> b));
    }

    private Map<String, Integer> getWorldCases(Data data) {
        return data.getNewCases().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, stringMapEntry -> stringMapEntry
                        .getValue()
                        .values()
                        .stream()
                        .mapToInt(i -> i).sum(), (a, b) -> b));
    }

    private Map<String, Integer> getWorldDeaths(Data data) {
        return data.getNewDeaths().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, stringMapEntry -> stringMapEntry
                        .getValue()
                        .values()
                        .stream()
                        .mapToInt(i -> i).sum(), (a, b) -> b));
    }

    private Map<String, Integer> getRegionCases(Data data, List<String> regionCountries) {
        Map<String, Integer> regionCasesByDate = new HashMap<>();
        data.getNewCases().forEach((key, value) -> regionCasesByDate.put(key, regionCountries
                .stream()
                .mapToInt(value::get)
                .sum()));

        return regionCasesByDate;
    }

    private Map<String, Integer> getRegionDeaths(Data data, List<String> regionCountries) {
        Map<String, Integer> regionDeathsByDate = new HashMap<>();
        data.getNewDeaths().forEach((key, value) -> regionDeathsByDate.put(key, regionCountries
                .stream()
                .mapToInt(value::get)
                .sum()));

        return regionDeathsByDate;
    }

    private void calculatePredictedConfirmedCases(Data data) {
        TreeMap<String, Map<String, Long>> map = new TreeMap<>(data.dateComparator());
        TreeMap<String, Map<String, Long>> result = new TreeMap<>(data.dateComparator());
        TreeMap<String, Map<String, List<Integer>>> prediction = new TreeMap<>(data.dateComparator());
        map.putAll(data.getConfirmedCases());
        prediction.putAll(data.getPredictionNewCases());
        Map<String, Long> lastDayData = map.lastEntry().getValue();
        for (Map.Entry<String, Map<String, List<Integer>>> stringMapEntry : prediction.entrySet()) {
            Map<String, Long> predictedDate = new HashMap<>();
            for (Map.Entry<String, Long> stringIntegerEntry : lastDayData.entrySet()) {
                if (stringMapEntry.getValue().get(stringIntegerEntry.getKey()) != null) {
                    predictedDate.put(stringIntegerEntry.getKey(), lastDayData.get(stringIntegerEntry.getKey()) +
                            stringMapEntry.getValue().get(stringIntegerEntry.getKey()).get(0));
                }
            }
            result.put(stringMapEntry.getKey(), predictedDate);
            lastDayData = predictedDate;
        }
        data.setPredictionConfirmedCases(result);
    }

    private void calculatePredictedConfirmedDeaths(Data data) {
        TreeMap<String, Map<String, Integer>> map = new TreeMap<>(data.dateComparator());
        TreeMap<String, Map<String, Integer>> result = new TreeMap<>(data.dateComparator());
        TreeMap<String, Map<String, List<Integer>>> prediction = new TreeMap<>(data.dateComparator());
        map.putAll(data.getDeaths());
        prediction.putAll(data.getPredictionNewDeaths());
        Map<String, Integer> lastDayData = map.lastEntry().getValue();
        for (Map.Entry<String, Map<String, List<Integer>>> stringMapEntry : prediction.entrySet()) {
            Map<String, Integer> predictedDate = new HashMap<>();
            for (Map.Entry<String, Integer> stringIntegerEntry : lastDayData.entrySet()) {
                if (stringMapEntry.getValue().get(stringIntegerEntry.getKey()) != null) {
                    predictedDate.put(stringIntegerEntry.getKey(), lastDayData.get(stringIntegerEntry.getKey()) +
                            stringMapEntry.getValue().get(stringIntegerEntry.getKey()).get(0));
                }
            }
            result.put(stringMapEntry.getKey(), predictedDate);
            lastDayData = predictedDate;
        }
        data.setPredictionConfirmedDeaths(result);
    }
}
