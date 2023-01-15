package com.sapiofan.predictions.services.impl;

import com.opencsv.CSVReader;
import com.sapiofan.predictions.entities.CountryData;
import com.sapiofan.predictions.entities.WorldData;
import com.sapiofan.predictions.services.FileReaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileReaderServiceImpl implements FileReaderService {

    private final DateTimeFormatter initFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private final DateTimeFormatter endFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final Logger log = LoggerFactory.getLogger(FileReaderServiceImpl.class);

    @Override
    public WorldData getWorldData() {
        WorldData worldData = new WorldData();

        File statisticsFolder = new File("src/main/resources/templates/predictions");
        File[] listOfFiles = statisticsFolder.listFiles();
        boolean header = true;

        for (File listOfFile : listOfFiles) {
            if (listOfFile.getName().contains("Read")) {
                continue;
            }
            try (CSVReader csvReader = new CSVReader(new FileReader(listOfFile))) {
                String date = LocalDate.parse(listOfFile.getName().substring(0, listOfFile.getName().indexOf(".")), initFormatter)
                        .format(endFormatter);
                Map<String, Integer> dataCases = new HashMap<>();
                Map<String, Integer> confirmedCases = new HashMap<>();
                Map<String, Integer> dataDeaths = new HashMap<>();
                Map<String, Integer> confirmedDeaths = new HashMap<>();
                String[] values;
                while ((values = csvReader.readNext()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    String area = values[0];
                    if (area.equals("World") || area.equals("Europe") || area.equals("Americas") || area.equals("Asia")
                            || area.equals("Oceania") || area.equals("Africa")) {
                        dataCases.put(area, Integer.parseInt(values[1]));
                        dataDeaths.put(area, Integer.parseInt(values[2]));
                        confirmedCases.put(area, Integer.parseInt(values[3]));
                        confirmedDeaths.put(area, Integer.parseInt(values[4]));
                    }
                }
                worldData.getWorldCases().put(date, dataCases);
                worldData.getWorldDeaths().put(date, dataDeaths);
                worldData.getConfirmedCases().put(date, confirmedCases);
                worldData.getConfirmedDeaths().put(date, confirmedDeaths);
            } catch (IOException e) {
                log.error("Something went wrong while reading CSV files for getting world statistics: " + e);
            }
            header = true;
        }

        return worldData;
    }

    @Override
    public CountryData getCountryData(String country) {
        CountryData countryData = new CountryData(country);

        File statisticsFolder = new File("src/main/resources/templates/predictions");
        File[] listOfFiles = statisticsFolder.listFiles();

        for (File listOfFile : listOfFiles) {
            if (listOfFile.getName().contains("Read")) {
                continue;
            }
            try (CSVReader csvReader = new CSVReader(new FileReader(listOfFile))) {
                String date = LocalDate.parse(listOfFile.getName().substring(0, listOfFile.getName().indexOf(".")), initFormatter)
                        .format(endFormatter);
                String[] values;
                while ((values = csvReader.readNext()) != null) {
                    if (values[0].equals(country)) {
                        countryData.getCountryCases().put(date, Integer.parseInt(values[1]));
                        countryData.getCountryDeaths().put(date, Integer.parseInt(values[2]));
                        countryData.getCountryConfirmedCases().put(date, Integer.parseInt(values[3]));
                        countryData.getCountryConfirmedDeaths().put(date, Integer.parseInt(values[4]));
                        break;
                    }
                }
            } catch (IOException e) {
                log.error("Something went wrong while reading CSV files for getting country statistics: " + e);
            }
        }

        return countryData;
    }
}
