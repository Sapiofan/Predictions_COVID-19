package com.sapiofan.predictions.services.impl;

import com.opencsv.CSVReader;
import com.sapiofan.predictions.entities.AllCountries;
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
import java.util.*;

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
        List<String> names = new ArrayList<>(Arrays.asList(Objects.requireNonNull(statisticsFolder.list())));
        names.remove(names.stream().filter(n -> n.contains("Read")).findFirst().get());
        names.sort((o1, o2) -> {
            LocalDate localDate1 = LocalDate.parse(o1.substring(0, o1.indexOf('.')), initFormatter);
            LocalDate localDate2 = LocalDate.parse(o2.substring(0, o2.indexOf('.')), initFormatter);
            if (localDate1.isAfter(localDate2)) {
                return 1;
            } else if (localDate1.isEqual(localDate2)) {
                return 0;
            }

            return -1;
        });
        String lastDay = names.get(0);
        LocalDate today = LocalDate.now();

        for (File listOfFile : listOfFiles) {
            if (listOfFile.getName().contains("Read") || listOfFile.getName().equals(lastDay)) {
                continue;
            }
            try (CSVReader csvReader = new CSVReader(new FileReader(listOfFile))) {
                String date = LocalDate.parse(listOfFile.getName().substring(0, listOfFile.getName().indexOf(".")), initFormatter)
                        .format(endFormatter);
                Map<String, List<Integer>> dataCases = new HashMap<>();
                Map<String, List<Integer>> confirmedCases = new HashMap<>();
                Map<String, List<Integer>> dataDeaths = new HashMap<>();
                Map<String, List<Integer>> confirmedDeaths = new HashMap<>();
                String[] values;
                while ((values = csvReader.readNext()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    String area = values[0];
                    if (area.equals("World") || area.equals("Europe") || area.equals("Americas") || area.equals("Asia")
                            || area.equals("Oceania") || area.equals("Africa")) {
                        List<Integer> cases = new ArrayList<>();
                        List<Integer> deaths = new ArrayList<>();
                        List<Integer> cCases = new ArrayList<>();
                        List<Integer> cDeaths = new ArrayList<>();
                        cases.add(Integer.parseInt(values[1]));
                        deaths.add(Integer.parseInt(values[2]));
                        cCases.add(Integer.parseInt(values[3]));
                        cDeaths.add(Integer.parseInt(values[4]));
                        if(values.length > 5 && !(values[5] == null || values[5].isEmpty())) {
                            cases.add(Integer.parseInt(values[5]));
                            cases.add(Integer.parseInt(values[6]));
                            deaths.add(Integer.parseInt(values[7]));
                            deaths.add(Integer.parseInt(values[8]));

                        }
                        dataCases.put(area, cases);
                        dataDeaths.put(area, deaths);
                        confirmedCases.put(area, cCases);
                        confirmedDeaths.put(area, cDeaths);
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
                        List<Integer> cases = new ArrayList<>();
                        List<Integer> deaths = new ArrayList<>();
                        List<Integer> cCases = new ArrayList<>();
                        List<Integer> cDeaths = new ArrayList<>();
                        cases.add(Integer.parseInt(values[1]));
                        deaths.add(Integer.parseInt(values[2]));
                        cCases.add(Integer.parseInt(values[3]));
                        cDeaths.add(Integer.parseInt(values[4]));
                        if(values.length > 5) {
                            cases.add(Integer.parseInt(values[5]));
                            cases.add(Integer.parseInt(values[6]));
                            deaths.add(Integer.parseInt(values[7]));
                            deaths.add(Integer.parseInt(values[8]));
                        }
                        countryData.getCountryCases().put(date, cases);
                        countryData.getCountryDeaths().put(date, deaths);
                        countryData.getCountryConfirmedCases().put(date, cCases);
                        countryData.getCountryConfirmedDeaths().put(date, cDeaths);
                        break;
                    }
                }
            } catch (IOException e) {
                log.error("Something went wrong while reading CSV files for getting country statistics: " + e);
            }
        }

        return countryData;
    }

    @Override
    public AllCountries getAllCountries() {
        AllCountries allCountries = new AllCountries();

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
                Map<String, Long> confirmedCases = new HashMap<>();
                Map<String, Integer> dataDeaths = new HashMap<>();
                Map<String, Integer> confirmedDeaths = new HashMap<>();
                String[] values;
                while ((values = csvReader.readNext()) != null) {
                    if (header) {
                        header = false;
                        continue;
                    }
                    String area = values[0];
                    if (!(area.equals("Europe") || area.equals("Americas") || area.equals("Asia")
                            || area.equals("Oceania") || area.equals("Africa"))) {
                        dataCases.put(area, Integer.parseInt(values[1]));
                        dataDeaths.put(area, Integer.parseInt(values[2]));
                        confirmedCases.put(area, Long.parseLong(values[3]));
                        confirmedDeaths.put(area, Integer.parseInt(values[4]));
                    }
                }
                allCountries.getNewCases().put(date, dataCases);
                allCountries.getNewDeaths().put(date, dataDeaths);
                allCountries.getConfirmedCases().put(date, confirmedCases);
                allCountries.getConfirmedDeaths().put(date, confirmedDeaths);
            } catch (IOException e) {
                log.error("Something went wrong while reading CSV files for getting %all countries% statistics: " + e);
            }
            header = true;
        }

        return allCountries;
    }
}
