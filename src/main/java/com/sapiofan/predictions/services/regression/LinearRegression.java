package com.sapiofan.predictions.services.regression;

import com.sapiofan.predictions.entities.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class LinearRegression {

    private final int DAYS = 30;


    /**
     * @param cases new cases of deaths for a certain country or world, where string is date and
     *              integer is number of cases
     *              <p>
     *              regressionFormula y = a + bx, where b = r * (Sy / Sx), a = y' - bx',
     *              r = sum((x - x') * (y - y'))/sqrt(sum((x - x')*(x - x')) * sum((y - y')*(y - y'))), x' = average label,
     * @return predicted cases for a certain country or world in general
     */
    public List<Double> SimpleRegression(Data data, Map<String, Integer> cases) {
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
                                data.getLabels().get(stringIntegerEntry.getKey()))) - stringIntegerEntry.getValue(), 2)).sum()
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
}
