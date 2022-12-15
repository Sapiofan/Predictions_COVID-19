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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
public class FileHandlerService {

    private static final Logger log = LoggerFactory.getLogger(Statistics.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final int DAYS = 30;

    public void downloadFilesForLastYear() {
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

    public void readData(Data data) {
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

    public void downloadFile(String urlStr, String file) throws IOException {
        URL url = new URL(urlStr);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
    }

    public void removeExtraFilesInStatistics(File statisticsFolder, String day) {
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
