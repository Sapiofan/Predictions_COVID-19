package com.sapiofan.predictions.services;

import com.opencsv.CSVReader;
import com.sapiofan.predictions.entities.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class Statistics {

    private static final Logger log = LoggerFactory.getLogger(Statistics.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final int DAYS = 30;

    public void getWorldStatistics() {
        Data data = new Data();
        downloadFilesForLastYear();
        readData(data);
        calculateNewCases(data);
        calculateNewDeaths(data);
        analyzeNewCasesForWorld(data);
//        data.getNewCases().entrySet().stream().findFirst()
//                .ifPresent(stringMapEntry -> stringMapEntry.getValue().forEach((key, value) -> analyzeDataForCountry(data, key)));
        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getNewDeaths().entrySet()) {
            for (Map.Entry<String, Integer> stringIntegerEntry : stringMapEntry.getValue().entrySet()) {
                analyzeDataForCountry(data, stringIntegerEntry.getKey());
            }
            break;
        }
    }

    private void downloadFilesForLastYear() {
        String urlString = "https://raw.githubusercontent.com/CSSEGISandData/" +
                "COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports/";
        String temp = urlString;
        String day = LocalDate.now().minusDays(1L).format(formatter);

        Path folder = Paths.get("src/main/resources/statistics/");
        File statisticsFolder = new File(folder.toString());

        for (int i = 0; i < DAYS; i++) {

            temp += day + ".csv";

            try {
                downloadFile(temp, folder + "/" + day + ".csv");
            } catch (IOException e) {
                log.error("Error while downloading files from github: " + e);
                continue;
            }

            day = LocalDate.parse(day, formatter).minusDays(1L).format(formatter);
            temp = urlString;
        }

        removeExtraFilesInStatistics(statisticsFolder, day);
    }

    private void readData(Data data) {
        File statisticsFolder = new File("src/main/resources/statistics/");
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

        String lastDay = LocalDate.now().minusDays(DAYS).format(formatter) + ".csv";
        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getConfirmedCases().entrySet()) {
            if (stringMapEntry.getKey().equals(lastDay)) {
                continue;
            }

            Map<String, Integer> previousDay = data.getConfirmedCases().get(LocalDate.parse(stringMapEntry.getKey()
                            .substring(0, stringMapEntry.getKey().lastIndexOf(".")), formatter)
                    .minusDays(1L).format(formatter) + ".csv");


            Map<String, Integer> newCasesDay = new HashMap<>();
            for (Map.Entry<String, Integer> stringIntegerEntry : stringMapEntry.getValue().entrySet()) {
                newCasesDay.put(stringIntegerEntry.getKey(),
                        stringIntegerEntry.getValue() - previousDay.get(stringIntegerEntry.getKey()));
            }

            newCases.put(stringMapEntry.getKey(), newCasesDay);
        }

        data.setNewCases(newCases);
    }

    private void calculateNewDeaths(Data data) {
        Map<String, Map<String, Integer>> newDeaths = new HashMap<>();

        String lastDay = LocalDate.now().minusDays(DAYS).format(formatter) + ".csv";
        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getDeaths().entrySet()) {
            if (stringMapEntry.getKey().equals(lastDay)) {
                continue;
            }

            Map<String, Integer> previousDay = data.getDeaths().get(LocalDate.parse(stringMapEntry.getKey()
                            .substring(0, stringMapEntry.getKey().lastIndexOf(".")), formatter)
                    .minusDays(1L).format(formatter) + ".csv");


            Map<String, Integer> newDeathsDay = new HashMap<>();
            for (Map.Entry<String, Integer> stringIntegerEntry : stringMapEntry.getValue().entrySet()) {
                newDeathsDay.put(stringIntegerEntry.getKey(),
                        stringIntegerEntry.getValue() - previousDay.get(stringIntegerEntry.getKey()));
            }

            newDeaths.put(stringMapEntry.getKey(), newDeathsDay);
        }

        data.setNewDeaths(newDeaths);
    }

    private void analyzeNewCasesForWorld(Data data) {
        Map<String, Integer> worldCases = getWorldCases(data);
        List<Double> simpleRegression = SimpleRegression(data, worldCases);
        double simpleRegressionErr = MSE(data, worldCases, simpleRegression);
        List<Double> descendentGradient = gradientDescent(data, worldCases);
        double descendentGradientErr = MSE(data, worldCases, descendentGradient);

        log.warn("Regression: " + simpleRegressionErr);
        log.warn("Gradient result: " + descendentGradientErr);

        List<Double> betterLine;

        if (simpleRegressionErr > descendentGradientErr) {
            betterLine = descendentGradient;
        } else {
            betterLine = simpleRegression;
        }

        for (int i = 1; i <= DAYS; i++) {
            Map<String, Integer> worldPrediction = new HashMap<>();
            worldPrediction.put("World", (int) getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            data.getPredictionNewCases().put(LocalDate.now().plusDays(i - 1).format(formatter) + ".csv", worldPrediction);
        }

        for (Map.Entry<String, Integer> stringIntegerEntry : worldCases.entrySet()) {
            log.warn("Existed data: " + stringIntegerEntry.getKey() + " : " + stringIntegerEntry.getValue());
        }

        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getPredictionNewCases().entrySet()) {
            log.warn("Predicted data: " + stringMapEntry.getKey() + " : " + stringMapEntry.getValue().get("World"));
        }
    }

    private void analyzeDataForCountry(Data data, String country) {
        Map<String, Integer> countryCases = getNewCasesOfCountry(data, country);
        List<Double> simpleRegression = SimpleRegression(data, countryCases);
        double simpleRegressionErr = MSE(data, countryCases, simpleRegression);
        List<Double> descendentGradient = gradientDescent(data, countryCases);
        double descendentGradientErr = MSE(data, countryCases, descendentGradient);

        log.warn("Regression: " + simpleRegressionErr);
        log.warn("Gradient result: " + descendentGradientErr);

        List<Double> betterLine;

        if (simpleRegressionErr > descendentGradientErr) {
            betterLine = descendentGradient;
        } else {
            betterLine = simpleRegression;
        }

        for (int i = 1; i <= DAYS; i++) {
            Map<String, Integer> worldPrediction = new HashMap<>();
            worldPrediction.put(country, (int) getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            data.getPredictionNewCases().put(LocalDate.now().plusDays(i - 1).format(formatter) + ".csv", worldPrediction);
        }
    }

    private Map<String, Integer> getNewCasesOfCountry(Data data, String country) {
        return data.getNewCases().entrySet()
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

//        int predictionCounter = DAYS + 1;

//        String day = LocalDate.now().format(formatter) + ".csv";
//        Map<String, Integer> predictDays = new HashMap<>();
//        for (int i = 0; i < DAYS; i++) {
//            predictDays.put(day, (int) Math.round(a + b * predictionCounter));
//            predictionCounter++;
//            day = LocalDate.parse(day, formatter).plusDays(1L).format(formatter);
//        }
        List<Double> thetas = new ArrayList<>();
        thetas.add(a);
        thetas.add(b);

        return thetas;
    }

    private double getPrediction(double slope, double intercept, int x_value) {
        return slope * x_value + intercept;
    }

    private double MSE(Data data, Map<String, Integer> cases, List<Double> thetas) {
        double sum = 0;

//        log.warn("start mse");
        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
//            log.warn(""+data.getLabels().get(stringIntegerEntry.getKey()));
            double prediction = getPrediction(thetas.get(1), thetas.get(0), data.getLabels().get(stringIntegerEntry.getKey()));
//            double prediction = getPrediction(thetas.get(1), thetas.get(0), DAYS + counter);
//            log.warn(stringIntegerEntry.getValue() + " : " + (int) prediction);
            sum += Math.pow((int) (prediction) - stringIntegerEntry.getValue(), 2);
        }

        return sum / (2.0 * cases.size());
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
