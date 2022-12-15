package com.sapiofan.predictions.services;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.sapiofan.predictions.entities.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class Statistics {

    private static final Logger log = LoggerFactory.getLogger(Statistics.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final int DAYS = 30;

    public Data getWorldStatistics() {
        Data data = new Data();
        downloadFilesForLastYear();
        readData(data);
        calculateNewCases(data);
        calculateNewDeaths(data);
        analyzeNewCasesForWorld(data);
        analyzeNewDeathsForWorld(data);
        data.getNewCases().entrySet().stream().findFirst()
                .ifPresent(stringMapEntry -> stringMapEntry.getValue().forEach((key, value) ->
                        analyzeNewCasesForCountry(data, key)));
        data.getNewDeaths().entrySet().stream().findFirst()
                .ifPresent(stringMapEntry -> stringMapEntry.getValue().forEach((key, value) ->
                        analyzeNewDeathsForCountry(data, key)));
        writeToCSV(data);

        return data;
    }

    private void downloadFilesForLastYear() {
        String urlString = "https://raw.githubusercontent.com/CSSEGISandData/" +
                "COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports/";
        String temp = urlString;
        String day = LocalDate.now().minusDays(1L).format(formatter);

        Path folder = Paths.get("src/main/resources/data/");
        File statisticsFolder = new File(folder.toString());

        for (int i = 0; i < DAYS; i++) {

            temp += day + ".csv";

            try {
                downloadFile(temp, folder + "/" + day + ".csv");
            } catch (IOException e) {
                log.error("Error while downloading files from github: " + e);
                day = LocalDate.parse(day, formatter).minusDays(1L).format(formatter);
                temp = urlString;
                continue;
            }

            day = LocalDate.parse(day, formatter).minusDays(1L).format(formatter);
            temp = urlString;
        }

        removeExtraFilesInStatistics(statisticsFolder, day);
    }

    private void readData(Data data) {
        File statisticsFolder = new File("src/main/resources/data/");
        File[] listOfFiles = statisticsFolder.listFiles();
        Map<String, Integer> labels = data.getLabels();
        int counter = 0;
        boolean header = true;

        for (File listOfFile : listOfFiles) {
            labels.put(listOfFile.getName(), counter);
            counter++;
            try (CSVReader csvReader = new CSVReader(new FileReader(listOfFile))) {
                String[] values;
                while ((values = csvReader.readNext()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    Map<String, Integer> dayCases = data.getConfirmedCases().get(listOfFile.getName());
                    Map<String, Integer> dayDeaths = data.getDeaths().get(listOfFile.getName());
                    if (dayCases != null && dayCases.get(values[3]) != null) {
                        dayCases.put(values[3], dayCases.get(values[3]) + Integer.parseInt(values[7]));
                        dayDeaths.put(values[3], dayDeaths.get(values[3]) + Integer.parseInt(values[8]));
                    } else if (dayCases != null && dayCases.get(values[3]) == null) {
                        dayCases.put(values[3], Integer.parseInt(values[7]));
                        dayDeaths.put(values[3], Integer.parseInt(values[8]));
                    } else {
                        dayCases = new HashMap<>();
                        dayDeaths = new HashMap<>();
                        dayCases.put(values[3], Integer.parseInt(values[7]));
                        dayDeaths.put(values[3], Integer.parseInt(values[8]));
                        data.getConfirmedCases().put(listOfFile.getName(), dayCases);
                        data.getDeaths().put(listOfFile.getName(), dayDeaths);
                    }
                }
            } catch (IOException e) {
                log.error("Something went wrong while reading CSV files: " + e);
                return;
            }
            header = true;
        }
    }

    private void calculateNewCases(Data data) {
        Map<String, Map<String, Integer>> newCases = new HashMap<>();

        boolean flag = false;

        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getConfirmedCases().entrySet()) {

            Map<String, Integer> previousDay = data.getConfirmedCases().get(LocalDate.parse(stringMapEntry.getKey()
                            .substring(0, stringMapEntry.getKey().lastIndexOf(".")), formatter)
                    .minusDays(1L).format(formatter) + ".csv");

            Map<String, Integer> newCasesDay = new HashMap<>();
            for (Map.Entry<String, Integer> stringIntegerEntry : stringMapEntry.getValue().entrySet()) {
                if (previousDay == null) {
                    flag = true;
                    break;
                }
                newCasesDay.put(stringIntegerEntry.getKey(),
                        stringIntegerEntry.getValue() - previousDay.get(stringIntegerEntry.getKey()));
            }
            if (flag) {
                flag = false;
                continue;
            }
            newCases.put(stringMapEntry.getKey(), newCasesDay);
        }

        data.setNewCases(newCases);
    }

    private void calculateNewDeaths(Data data) {
        Map<String, Map<String, Integer>> newDeaths = new HashMap<>();
        boolean flag = false;

        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getDeaths().entrySet()) {

            Map<String, Integer> previousDay = data.getDeaths().get(LocalDate.parse(stringMapEntry.getKey()
                            .substring(0, stringMapEntry.getKey().lastIndexOf(".")), formatter)
                    .minusDays(1L).format(formatter) + ".csv");


            Map<String, Integer> newDeathsDay = new HashMap<>();
            for (Map.Entry<String, Integer> stringIntegerEntry : stringMapEntry.getValue().entrySet()) {
                if (previousDay == null) {
                    flag = true;
                    break;
                }
                newDeathsDay.put(stringIntegerEntry.getKey(),
                        stringIntegerEntry.getValue() - previousDay.get(stringIntegerEntry.getKey()));
            }
            if (flag) {
                flag = false;
                continue;
            }
            newDeaths.put(stringMapEntry.getKey(), newDeathsDay);
        }

        data.setNewDeaths(newDeaths);
    }

    private void analyzeNewCasesForWorld(Data data) {
        Map<String, Integer> worldCases = getWorldCases(data);
        List<Double> betterLine = analyzeData(data, worldCases);

        for (int i = 1; i <= DAYS; i++) {
            Map<String, Integer> worldPrediction = new HashMap<>();
            worldPrediction.put("World", (int) getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            data.getPredictionNewCases().put(LocalDate.now().plusDays(i - 1).format(formatter) + ".csv", worldPrediction);
        }
    }

    private void analyzeNewDeathsForWorld(Data data) {
        Map<String, Integer> worldCases = getWorldDeaths(data);
        List<Double> betterLine = analyzeData(data, worldCases);

        for (int i = 1; i <= DAYS; i++) {
            Map<String, Integer> worldPrediction = new HashMap<>();
            worldPrediction.put("World", (int) getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            data.getPredictionNewDeaths().put(LocalDate.now().plusDays(i - 1).format(formatter) + ".csv", worldPrediction);
        }
    }

    private void analyzeNewCasesForCountry(Data data, String country) {
        Map<String, Integer> countryCases = getNewCasesOfCountry(data, country);
        List<Double> betterLine = analyzeData(data, countryCases);

        for (int i = 1; i <= DAYS; i++) {
            String date = LocalDate.now().plusDays(i - 1).format(formatter) + ".csv";
            Map<String, Integer> countryPrediction = new HashMap<>();
            for (Map.Entry<String, Map<String, Integer>> s : data.getPredictionNewCases().entrySet()) {
                if(s.getKey().equals(date)) {
                    countryPrediction = s.getValue();
                    break;
                }
            }
            countryPrediction.put(country, (int) getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            data.getPredictionNewCases().replace(date, countryPrediction);
        }
    }

    private void analyzeNewDeathsForCountry(Data data, String country) {
        Map<String, Integer> countryDeaths = getNewDeathsOfCountry(data, country);
        List<Double> betterLine = analyzeData(data, countryDeaths);

        for (int i = 1; i <= DAYS; i++) {
            String date = LocalDate.now().plusDays(i - 1).format(formatter) + ".csv";
            Map<String, Integer> countryPrediction = new HashMap<>();
            for (Map.Entry<String, Map<String, Integer>> s : data.getPredictionNewCases().entrySet()) {
                if(s.getKey().equals(date)) {
                    countryPrediction = s.getValue();
                    break;
                }
            }
            countryPrediction.put(country, (int) getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            data.getPredictionNewDeaths().replace(date, countryPrediction);
        }
    }

    private void writeToCSV(Data data) {
        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getNewCases().entrySet()) {
            File file = new File("src/main/resources/templates/predictions/"
                    + stringMapEntry.getKey());
            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                List<String[]> csvData = new ArrayList<>();
                csvData.add(new String[]{"Country", "Cases", "Deaths"});
                for (Map.Entry<String, Integer> entry : stringMapEntry.getValue().entrySet()) {
                    String key = entry.getKey();
                    Integer value = entry.getValue();
                    csvData.add(new String[]{key, String.valueOf(value),
                            String.valueOf(data.getNewDeaths().get(stringMapEntry.getKey()).get(key))});
                }
                writer.writeAll(csvData);
            } catch (IOException e) {
                log.error("Error while writing data to CSV file: " + e);
                return;
            }
        }

        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getPredictionNewCases().entrySet()) {
            File file = new File("src/main/resources/templates/predictions/"
                    + stringMapEntry.getKey());
            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                List<String[]> csvData = new ArrayList<>();
                csvData.add(new String[]{"Country", "Cases", "Deaths"});
                for (Map.Entry<String, Integer> entry : stringMapEntry.getValue().entrySet()) {
                    String key = entry.getKey();
                    Integer value = entry.getValue();
                    csvData.add(new String[]{key, String.valueOf(value),
                            String.valueOf(data.getPredictionNewDeaths().entrySet()
                                    .stream()
                                    .filter(mapEntry -> mapEntry.getKey().equals(stringMapEntry.getKey()))
                                    .findFirst().map(Map.Entry::getValue).orElse(null)
                                    .get(key))});
                }
                writer.writeAll(csvData);
            } catch (IOException e) {
                log.error("Error while writing data to CSV file: " + e);
                return;
            }
        }

        removeExtraFilesInStatistics(new File("src/main/resources/templates/predictions/"),
                LocalDate.now().minusDays(DAYS + 1L).format(formatter));
    }

    private List<Double> analyzeData(Data data, Map<String, Integer> countryCases) {
        List<Double> simpleRegression = SimpleRegression(data, countryCases);
        double simpleRegressionErr = MSE(data, countryCases, simpleRegression);
        List<Double> descendentGradient = gradientDescent(data, countryCases);
        double descendentGradientErr = MSE(data, countryCases, descendentGradient);

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
        return data.getNewCases().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, stringMapEntry ->
                        stringMapEntry.getValue().get(country), (a, b) -> b));
    }

    private Map<String, Integer> getNewDeathsOfCountry(Data data, String country) {
        return data.getNewDeaths().entrySet()
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


    /**
     * @param cases new cases of deaths for a certain country or world, where string is date and
     *              integer is number of cases
     *              <p>
     *              regressionFormula y = a + bx, where b = r * (Sy / Sx), a = y' - bx',
     *              r = sum((x - x') * (y - y'))/sqrt(sum((x - x')*(x - x')) * sum((y - y')*(y - y'))), x' = average label,
     * @return predicted cases for a certain country or world in general
     */
    private List<Double> SimpleRegression(Data data, Map<String, Integer> cases) {
        double a, b;
        double averageX, averageY;
        Map<Integer, Double> xDiff;
        Map<Integer, Double> yDiff;
        double sum = IntStream.rangeClosed(1, DAYS).asDoubleStream().sum();
        double newCasesSum = cases.values().stream().mapToDouble(v -> v).sum();
        double sumXYDiffs, sumXDiffInSquare, sumYDiffInSquare, correlationCoefficient, Sy, Sx;

        averageX = sum / DAYS;
        averageY = newCasesSum / DAYS;

        xDiff = cases.entrySet().stream()
                .collect(Collectors.toMap(stringIntegerEntry ->
                                data.getLabels().get(stringIntegerEntry.getKey()),
                        stringIntegerEntry ->
                                data.getLabels().get(stringIntegerEntry.getKey()) - averageX, (i, k) -> k));

        yDiff = cases.entrySet().stream()
                .collect(Collectors.toMap(stringIntegerEntry ->
                                data.getLabels().get(stringIntegerEntry.getKey()),
                        stringIntegerEntry -> stringIntegerEntry.getValue() - averageY, (i, k) -> k));

        sumXYDiffs = xDiff.entrySet().stream()
                .mapToDouble(integerDoubleEntry ->
                        integerDoubleEntry.getValue() * yDiff.get(integerDoubleEntry.getKey()))
                .sum();

        sumXDiffInSquare = xDiff.values().stream().mapToDouble(aDouble -> aDouble * aDouble).sum();

        sumYDiffInSquare = yDiff.values().stream().mapToDouble(aDouble -> aDouble * aDouble).sum();

        correlationCoefficient = sumXYDiffs / (Math.sqrt(sumXDiffInSquare * sumYDiffInSquare));

        Sy = Math.sqrt(sumYDiffInSquare / (DAYS - 1));
        Sx = Math.sqrt(sumXDiffInSquare / (DAYS - 1));

        b = correlationCoefficient * (Sy / Sx);
        a = averageY - b * averageX;

        List<Double> thetas = new ArrayList<>();
        thetas.add(a);
        thetas.add(b);

        return thetas;
    }

    private double getPrediction(double slope, double intercept, int x_value) {
        double result = slope * x_value + intercept;
        if (result < 0) {
            return 0;
        }
        return result;
    }

    private double MSE(Data data, Map<String, Integer> cases, List<Double> thetas) {
        return cases.entrySet().stream()
                .mapToDouble(stringIntegerEntry ->
                        Math.pow((int) (getPrediction(thetas.get(1), thetas.get(0),
                                data.getLabels().get(stringIntegerEntry.getKey()))) - stringIntegerEntry.getValue(), 2)).sum()
                / (2.0 * cases.size());
    }

    private List<Double> gradientDescent(Data data, Map<String, Integer> cases) {
        int iterations = 1000;
        List<Double> thetas = new ArrayList<>();
        thetas.add((double) 0);
        thetas.add((double) 0);

        for (int i = 0; i < iterations; i++) {
//            if (i % 50 == 0) {
//                log.warn("Current iteration: " + i);
//                log.warn("Cost: " + MSE(data, cases, thetas));
//                log.warn("Theta1: " + thetas.get(0));
//                log.warn("Theta2: " + thetas.get(1));
//            }
            calculateThetas(data, cases, thetas);
        }

        return thetas;
    }

    private void calculateThetas(Data data, Map<String, Integer> cases, List<Double> thetas) {
        double interceptGradient = 0, slopeGradient = 0, alpha = 0.001;

        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
            double predicted = getPrediction(thetas.get(1), thetas.get(0), data.getLabels().get(stringIntegerEntry.getKey()));
            interceptGradient += predicted - stringIntegerEntry.getValue();
            slopeGradient += (predicted - stringIntegerEntry.getValue()) * data.getLabels().get(stringIntegerEntry.getKey());
        }

        thetas.set(0, (thetas.get(0) - alpha * interceptGradient));
        thetas.set(1, (thetas.get(1) - alpha * (1.0 / cases.size()) * slopeGradient));
    }

    private void downloadFile(String urlStr, String file) throws IOException {
        URL url = new URL(urlStr);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
    }

    private void removeExtraFilesInStatistics(File statisticsFolder, String day) {
        File[] listOfFiles = statisticsFolder.listFiles();
        LocalDate lastDayFromNow = LocalDate.parse(day, formatter);

        IntStream.range(0, listOfFiles.length)
                .filter(i -> Character.isDigit(listOfFiles[i].getName().charAt(0)) &&
                        lastDayFromNow.isAfter(LocalDate.parse(listOfFiles[i].getName()
                                .substring(0, listOfFiles[i].getName().lastIndexOf(".")), formatter)))
                .filter(i -> !listOfFiles[i].delete())
                .mapToObj(i -> "Can't remove file: " + listOfFiles[i].getName()).forEach(log::error);
    }
}
