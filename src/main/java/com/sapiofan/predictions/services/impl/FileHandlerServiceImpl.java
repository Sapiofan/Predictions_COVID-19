package com.sapiofan.predictions.services.impl;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.sapiofan.predictions.entities.Data;
import com.sapiofan.predictions.services.FileHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class FileHandlerServiceImpl implements FileHandlerService {

    private static final Logger log = LoggerFactory.getLogger(FileHandlerServiceImpl.class);

    private static final String CSV_EXTENSION = ".csv";
    private static final String RESOURCES = "src/main/resources/";
    private static final String PREDICTIONS_FOLDER = "templates/predictions/";
    private static final String DATA_FOLDER = "data/";

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private int DAYS = 181;

    @Autowired
    private Utils utils;

    @Override
    public void downloadFilesWithData(int days) {
        DAYS = days + 1;
        String urlString = "https://raw.githubusercontent.com/CSSEGISandData/" +
                "COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports/";
        String temp = urlString;
        String day = LocalDate.now().minusDays(1L).format(formatter);

        Path folder = Paths.get(RESOURCES + DATA_FOLDER);
        File statisticsFolder = new File(folder.toString());
        List<String> fileNames = sortFilesByDate(statisticsFolder);

        LocalDate localDate1 = LocalDate.parse(fileNames.get(fileNames.size() - 1)
                .substring(0, fileNames.get(fileNames.size() - 1).indexOf('.')), formatter);
        LocalDate localDate2 = LocalDate.now();

        int count = fileNames.size() < DAYS ? DAYS : Math.min((int) ChronoUnit.DAYS.between(localDate1, localDate2) + 1, DAYS);
        count = Math.max(count, 15);

        for (int i = 0; i < count; i++) {

            temp += day + CSV_EXTENSION;

            try {
                downloadFile(temp, folder + "/" + day + CSV_EXTENSION);
            } catch (IOException e) {
                log.error("Error while downloading fileNames from github: " + e);
                day = LocalDate.parse(day, formatter).minusDays(1L).format(formatter);
                temp = urlString;
                continue;
            }

            day = LocalDate.parse(day, formatter).minusDays(1L).format(formatter);
            temp = urlString;
        }

        removeExtraFilesInStatistics(fileNames,
                new ArrayList<>(Arrays.asList(Objects.requireNonNull(statisticsFolder.listFiles()))));
    }

    @Override
    public void readData(Data data) {
        File statisticsFolder = new File(RESOURCES + DATA_FOLDER);
        File[] listOfFiles = statisticsFolder.listFiles();
        List<String> sortedListOfFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(statisticsFolder.list())));
        sortedListOfFiles = sortedListOfFiles.stream().filter(f -> f.contains(CSV_EXTENSION) &&
                !utils.compareDateAndString(f, DAYS, formatter)).collect(Collectors.toList());
        sortedListOfFiles.sort(data.dateComparator());
        int counter = 0;
        Map<String, Integer> labels = data.getLabelsByDate();
        for (String sortedListOfFile : sortedListOfFiles) {
            labels.put(sortedListOfFile, counter++);
        }
        boolean header = true;

        for (File listOfFile : listOfFiles) {
            try (CSVReader csvReader = new CSVReader(new FileReader(listOfFile))) {
                String[] values;
                while ((values = csvReader.readNext()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    Map<String, Long> dayCases = data.getConfirmedCases().get(listOfFile.getName());
                    Map<String, Integer> dayDeaths = data.getDeaths().get(listOfFile.getName());
                    if (dayCases != null && dayCases.get(values[3]) != null) {
                        dayCases.put(values[3], dayCases.get(values[3]) + Integer.parseInt(values[7]));
                        dayDeaths.put(values[3], dayDeaths.get(values[3]) + Integer.parseInt(values[8]));
                    } else if (dayCases != null && dayCases.get(values[3]) == null) {
                        dayCases.put(values[3], Long.parseLong(values[7]));
                        dayDeaths.put(values[3], Integer.parseInt(values[8]));
                    } else {
                        dayCases = new HashMap<>();
                        dayDeaths = new HashMap<>();
                        dayCases.put(values[3], Long.parseLong(values[7]));
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

    @Override
    public void writeToCSV(Data data) {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(12);
        for (Map.Entry<String, Map<String, Integer>> stringMapEntry : data.getNewCases().entrySet()) {
            executor.execute(() -> {
                try (CSVWriter writer = new CSVWriter(new FileWriter(RESOURCES + PREDICTIONS_FOLDER
                        + stringMapEntry.getKey()))) {
                    List<String[]> csvData = new ArrayList<>();
                    csvData.add(new String[]{"Country", "Cases", "Deaths", "Confirmed cases", "Confirmed deaths"});
                    stringMapEntry.getValue().entrySet()
                            .stream()
                            .map(entry -> new String[]{entry.getKey(), String.valueOf(entry.getValue()),
                                    String.valueOf(data.getNewDeaths().get(stringMapEntry.getKey())
                                            .get(entry.getKey())),
                                    String.valueOf(data.getConfirmedCases().get(stringMapEntry.getKey()).get(entry.getKey())),
                                    String.valueOf(data.getDeaths().get(stringMapEntry.getKey()).get(entry.getKey()))})
                            .forEach(csvData::add);
                    writer.writeAll(csvData);
                } catch (IOException e) {
                    log.error("Error while writing data to CSV file: " + e);
                }
            });
        }

        for (Map.Entry<String, Map<String, List<Integer>>> stringMapEntry : data.getPredictionNewCases().entrySet()) {
            executor.execute(() -> {
                try (CSVWriter writer = new CSVWriter(new FileWriter(RESOURCES + PREDICTIONS_FOLDER
                        + stringMapEntry.getKey()))) {
                    List<String[]> csvData = new ArrayList<>();
                    csvData.add(new String[]{"Country", "Cases", "Deaths", "Confirmed cases", "Confirmed deaths",
                            "Low bound cases", "High bound cases", "Low bound deaths", "High bound deaths"});
                    stringMapEntry.getValue().entrySet().stream().map(stringIntegerEntry -> new String[]{
                                    stringIntegerEntry.getKey(),
                                    String.valueOf(stringIntegerEntry.getValue().get(0)),
                                    String.valueOf(data.getPredictionNewDeaths()
                                            .get(stringMapEntry.getKey())
                                            .get(stringIntegerEntry.getKey()).get(0)),
                                    String.valueOf(data.getPredictionConfirmedCases()
                                            .get(stringMapEntry.getKey())
                                            .get(stringIntegerEntry.getKey())),
                                    String.valueOf(data.getPredictionConfirmedDeaths()
                                            .get(stringMapEntry.getKey())
                                            .get(stringIntegerEntry.getKey())),
                                    String.valueOf(stringIntegerEntry.getValue().get(1)),
                                    String.valueOf(stringIntegerEntry.getValue().get(2)),
                                    String.valueOf(data.getPredictionNewDeaths()
                                            .get(stringMapEntry.getKey())
                                            .get(stringIntegerEntry.getKey()).get(1)),
                                    String.valueOf(data.getPredictionNewDeaths()
                                            .get(stringMapEntry.getKey())
                                            .get(stringIntegerEntry.getKey()).get(2))})
                            .forEach(csvData::add);
                    writer.writeAll(csvData);
                } catch (IOException e) {
                    log.error("Error while writing data to CSV file: " + e);
                }
            });
        }

        File forecastFolder = new File(RESOURCES + PREDICTIONS_FOLDER);

        removeExtraFilesInStatistics(sortFilesByDate(forecastFolder),
                new ArrayList<>(Arrays.asList(Objects.requireNonNull(forecastFolder.listFiles()))));
    }

    private List<String> sortFilesByDate(File folder) {
        List<String> fileNames = new ArrayList<>(Arrays.asList(Objects.requireNonNull(folder.list())));
        fileNames.removeIf(s -> !s.matches(".*[0-9].*"));

        fileNames.sort((o1, o2) -> {
            LocalDate localDate1 = LocalDate.parse(o1.substring(0, o1.indexOf('.')), formatter);
            LocalDate localDate2 = LocalDate.parse(o2.substring(0, o2.indexOf('.')), formatter);
            return localDate1.isAfter(localDate2) ? 1 : -1;
        });

        return fileNames;
    }

    private void downloadFile(String urlStr, String file) throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(new URL(urlStr).openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
    }

    private void removeExtraFilesInStatistics(List<String> fileNames, List<File> files) {
        fileNames.stream()
                .filter(file ->
                        LocalDate.parse(file.substring(0, file.indexOf('.')), formatter)
                                .isBefore(LocalDate.now().minusDays(DAYS)))
                .forEach(file -> files.get(files.indexOf(
                                files.stream()
                                        .filter(file1 -> file1.getName().equals(file))
                                        .findFirst()
                                        .get()))
                        .delete());
    }

    @Override
    public List<String> countriesFromFile() {
        List<String> countries;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(RESOURCES + "countries.txt"))) {
            countries = bufferedReader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error while getting countries list: " + e);
            return null;
        }

        return countries;
    }

    @Override
    public Map<String, String> countriesByRegions() {
        Map<String, String> countries;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(RESOURCES + "regions.txt"))) {
            countries = bufferedReader.lines()
                    .map(line -> line.split("#"))
                    .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1], (a, b) -> b));
        } catch (IOException e) {
            log.error("Error while getting countries list: " + e);
            return null;
        }

        return countries;
    }
}
