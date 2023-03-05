package com.sapiofan.predictions.services.regression;

import com.sapiofan.predictions.entities.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LinearRegression {

    private final int DAYS = 30;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private static final String WORLD = "World";
    private static final String CSV_EXTENSION = ".csv";


    /**
     * @param cases new cases of deaths for a certain country or world, where string is date and
     *              integer is number of cases
     *              <p>
     *              regressionFormula y = a + bx, where b = r * (Sy / Sx), a = y' - bx',
     *              r = sum((x - x') * (y - y'))/sqrt(sum((x - x')*(x - x')) * sum((y - y')*(y - y'))), x' = average label,
     * @return predicted cases for a certain country or world in general
     */
    public List<Double> simpleRegression(Data data, Map<String, Integer> cases) {
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
                                data.getLabelsByDate().get(stringIntegerEntry.getKey()),
                        stringIntegerEntry ->
                                data.getLabelsByDate().get(stringIntegerEntry.getKey()) - averageX, (i, k) -> k));

        yDiff = cases.entrySet().stream()
                .collect(Collectors.toMap(stringIntegerEntry ->
                                data.getLabelsByDate().get(stringIntegerEntry.getKey()),
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

    public double getPrediction(double slope, double intercept, int x_value) {
        double result = slope * x_value + intercept;
        if (result < 0) {
            return 0;
        }
        return result;
    }

    public double MSE(Data data, Map<String, Integer> cases, List<Double> thetas) {
        return cases.entrySet().stream()
                .mapToDouble(stringIntegerEntry ->
                        Math.pow((int) (getPrediction(thetas.get(1), thetas.get(0),
                                data.getLabelsByDate().get(stringIntegerEntry.getKey()))) - stringIntegerEntry.getValue(), 2)).sum()
                / (2.0 * cases.size());
    }

    public List<Double> gradientDescent(Data data, Map<String, Integer> cases) {
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

        gradientDescentResult(data, cases, thetas);

        return thetas;
    }

    public void analyzeNewCasesForWorld(Data data, Map<String, Integer> worldCases) {
        List<Double> betterLine = analyzeData(data, worldCases);

        for (int i = 1; i <= DAYS; i++) {
            Map<String, List<Integer>> worldPrediction = new HashMap<>();
            List<Integer> list = new ArrayList<>();
            list.add((int) getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            worldPrediction.put(WORLD, list);
            data.getPredictionNewCases().put(LocalDate.now().plusDays(i - 1).format(formatter) + CSV_EXTENSION, worldPrediction);
        }
    }

    public void analyzeNewDeathsForWorld(Data data, Map<String, Integer> worldCases) {
        List<Double> betterLine = analyzeData(data, worldCases);

        for (int i = 1; i <= DAYS; i++) {
            Map<String, List<Integer>> worldPrediction = new HashMap<>();
            List<Integer> list = new ArrayList<>();
            list.add((int) getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            worldPrediction.put(WORLD, list);
            data.getPredictionNewDeaths().put(LocalDate.now().plusDays(i - 1).format(formatter) + CSV_EXTENSION, worldPrediction);
        }
    }

    public void analyzeNewCasesForCountry(Data data, String country, Map<String, Integer> countryCases) {
        List<Double> betterLine = analyzeData(data, countryCases);

        for (int i = 1; i <= DAYS; i++) {
            String date = LocalDate.now().plusDays(i - 1).format(formatter) + CSV_EXTENSION;
            Map<String, List<Integer>> countryPrediction = data.getPredictionNewCases().entrySet()
                    .stream().filter(s -> s.getKey().equals(date))
                    .findFirst().map(Map.Entry::getValue).orElse(new HashMap<>());
            List<Integer> list = new ArrayList<>();
            list.add((int) getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            countryPrediction.put(country, list);
            data.getPredictionNewCases().replace(date, countryPrediction);
        }
    }

    public void analyzeNewDeathsForCountry(Data data, String country, Map<String, Integer> countryDeaths) {
        List<Double> betterLine = analyzeData(data, countryDeaths);

        for (int i = 1; i <= DAYS; i++) {
            String date = LocalDate.now().plusDays(i - 1).format(formatter) + CSV_EXTENSION;
            Map<String, List<Integer>> countryPrediction = data.getPredictionNewDeaths()
                    .entrySet().stream().filter(s -> s.getKey().equals(date))
                    .findFirst().map(Map.Entry::getValue).orElse(new HashMap<>());
            List<Integer> list = new ArrayList<>();
            list.add((int) getPrediction(betterLine.get(1), betterLine.get(0), DAYS + i));
            countryPrediction.put(country, list);
            data.getPredictionNewDeaths().replace(date, countryPrediction);
        }
    }

    private List<Double> analyzeData(Data data, Map<String, Integer> countryCases) {
        List<Double> simpleRegression = simpleRegression(data, countryCases);
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

    private void calculateThetas(Data data, Map<String, Integer> cases, List<Double> thetas) {
        double interceptGradient = 0, slopeGradient = 0, alpha = 0.001;

        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
            double predicted = getPrediction(thetas.get(1), thetas.get(0), data.getLabelsByDate().get(stringIntegerEntry.getKey()));
            interceptGradient += predicted - stringIntegerEntry.getValue();
            slopeGradient += (predicted - stringIntegerEntry.getValue()) * data.getLabelsByDate().get(stringIntegerEntry.getKey());
        }

        thetas.set(0, (thetas.get(0) - alpha * interceptGradient));
        thetas.set(1, (thetas.get(1) - alpha * (1.0 / cases.size()) * slopeGradient));
    }

    private void gradientDescentResult(Data data, Map<String, Integer> cases, List<Double> thetas) {
        TreeMap<String, Integer> testCases = new TreeMap<>(data.dateComparator());
        for (Map.Entry<String, Integer> stringIntegerEntry : cases.entrySet()) {
            testCases.put(stringIntegerEntry.getKey(), (int) getPrediction(thetas.get(0), thetas.get(1),
                    data.getLabelsByDate().get(stringIntegerEntry.getKey())));
        }
        int counter = data.getLabelsByDate().get(testCases.lastKey());
        for (int i = 1; i <= 28; i++) {
            testCases.put(LocalDate.parse(testCases.lastKey().substring(0, testCases.lastKey().indexOf(".")), formatter)
                    .plusDays(i).format(formatter) + CSV_EXTENSION, (int) getPrediction(thetas.get(0), thetas.get(1), ++counter));
        }
        for (Map.Entry<String, Integer> stringIntegerEntry : testCases.entrySet()) {
            System.out.println(stringIntegerEntry.getKey() + " : " + stringIntegerEntry.getValue());
        }
    }
}
