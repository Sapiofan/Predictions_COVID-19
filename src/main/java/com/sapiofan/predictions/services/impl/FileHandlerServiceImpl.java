package com.sapiofan.predictions.services.impl;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.sapiofan.predictions.entities.Data;
import com.sapiofan.predictions.services.FileHandlerService;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

@Service
public class FileHandlerServiceImpl implements FileHandlerService {

    private static final Logger log = LoggerFactory.getLogger(FileHandlerServiceImpl.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final int DAYS = 120;

    @Override
    public void downloadFilesWithData() {
        String urlString = "https://raw.githubusercontent.com/CSSEGISandData/" +
                "COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports/";
        String temp = urlString;
        String day = LocalDate.now().minusDays(1L).format(formatter);

        Path folder = Paths.get("src/main/resources/data/");
        File statisticsFolder = new File(folder.toString());
        List<String> fileNames = sortFilesByDate(statisticsFolder);
        List<File> files = new ArrayList<>(Arrays.asList(Objects.requireNonNull(statisticsFolder.listFiles())));

        LocalDate localDate1 = LocalDate.parse(fileNames.get(fileNames.size() - 1)
                .substring(0, fileNames.get(fileNames.size() - 1).indexOf('.')), formatter);
        LocalDate localDate2 = LocalDate.now();

        int count = fileNames.size() < DAYS ? DAYS : Math.min((int) ChronoUnit.DAYS.between(localDate1, localDate2) + 1, DAYS);

        for (int i = 0; i < count; i++) {

            temp += day + ".csv";

            try {
                downloadFile(temp, folder + "/" + day + ".csv");
            } catch (IOException e) {
                log.error("Error while downloading fileNames from github: " + e);
                day = LocalDate.parse(day, formatter).minusDays(1L).format(formatter);
                temp = urlString;
                continue;
            }

            day = LocalDate.parse(day, formatter).minusDays(1L).format(formatter);
            temp = urlString;
        }

        removeExtraFilesInStatistics(fileNames, files);
    }

    @Override
    public void readData(Data data) {
        File statisticsFolder = new File("src/main/resources/data/");
        File[] listOfFiles = statisticsFolder.listFiles();
        Map<String, Integer> labels = data.getLabelsByDate();
        int counter = 0;
        boolean header = true;

        for (File listOfFile : listOfFiles) {
            if(listOfFile.getName().contains(".csv")) {
                labels.put(listOfFile.getName(), counter);
            }
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

    @Override
    public void writeToCSV(Data data) {
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
                Set<String> set = new TreeSet<>(stringMapEntry.getValue().keySet());
                for (String s : set) {
                    Map<String, Integer> map = data.getPredictionNewDeaths().entrySet()
                            .stream()
                            .filter(mapEntry -> mapEntry.getKey().equals(stringMapEntry.getKey()))
                            .findFirst()
                            .map(Map.Entry::getValue)
                            .orElse(null);
                    if(map == null) {
                        log.error("Couldn't find: " + s + " in deaths");
                        continue;
                    }
                    if(map.get(s) == 2147483647) {
                        log.error(s);
                        log.error(""+map);
                    }
                    csvData.add(new String[]{s, String.valueOf(stringMapEntry.getValue().get(s)),
                            String.valueOf(map.get(s))});
                }
//                for (Map.Entry<String, Integer> entry : stringMapEntry.getValue().entrySet()) {
//                    String key = entry.getKey();
//                    Integer value = entry.getValue();
//                    csvData.add(new String[]{key, String.valueOf(value),
//                            String.valueOf(data.getPredictionNewDeaths().entrySet()
//                                    .stream()
//                                    .filter(mapEntry -> mapEntry.getKey().equals(stringMapEntry.getKey()))
//                                    .findFirst().map(Map.Entry::getValue).orElse(null)
//                                    .get(key))});
//                }
                writer.writeAll(csvData);
            } catch (IOException e) {
                log.error("Error while writing data to CSV file: " + e);
                return;
            }
        }

        File forecastFolder = new File("src/main/resources/templates/predictions/");
        List<String> fileNames = sortFilesByDate(forecastFolder);
        List<File> files = new ArrayList<>(Arrays.asList(Objects.requireNonNull(forecastFolder.listFiles())));

        removeExtraFilesInStatistics(fileNames, files);
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
        URL url = new URL(urlStr);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
    }

    private void removeExtraFilesInStatistics(List<String> fileNames, List<File> files) {
        LocalDate lastDayFromNow = LocalDate.now().minusDays(DAYS);
        fileNames.stream()
                .filter(file ->
                        LocalDate.parse(file.substring(0, file.indexOf('.')), formatter).isBefore(lastDayFromNow))
                .forEach(file -> files.get(files.indexOf(
                        files.stream()
                        .filter(file1 -> file1.getName().equals(file))
                        .findFirst().get())).delete());
    }

    @Override
    public List<String> countriesFromFile() {
        List<String> countries = new ArrayList<>();
        File file = new File("src/main/resources/countries.txt");
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                countries.add(line);
            }
        } catch (IOException e) {
            log.error("Error while getting countries list: " + e);
        }

        return countries;
    }
}
