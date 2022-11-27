package com.sapiofan.predictions.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.IntStream;

@Service
public class Statistics {

    private static final Logger log = LoggerFactory.getLogger(Statistics.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    public void getWorldStatistics() {
        downloadFilesForLastYear();
    }

    private void downloadFilesForLastYear() {
        String urlString = "https://raw.githubusercontent.com/CSSEGISandData/" +
                "COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports/";
        String temp = urlString;
        String day = LocalDate.now().minusDays(1L).format(formatter);
        log.info(day);

        Path folder = Paths.get("src/main/resources/statistics/");
        File statisticsFolder = new File(folder.toString());

        for (int i = 0; i < 10; i++) {

            temp += day + ".csv";

            try {
                downloadFile(temp, folder + "/" + day + ".csv");
            } catch (IOException e) {
                log.error("Error while downloading files from github: " + e);
                continue;
            }

            day = LocalDate.parse(day, formatter).minusDays(1L).format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
            temp = urlString;
        }

        removeExtraFilesInStatistics(statisticsFolder, day);
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
                .filter(i -> listOfFiles[i].getName().charAt(0) != 'R' &&
                        lastDayFromNow.isAfter(LocalDate.parse(listOfFiles[i].getName()
                        .substring(0, listOfFiles[i].getName().lastIndexOf(".")), formatter)))
                .filter(i -> !listOfFiles[i].delete())
                .mapToObj(i -> "Can't remove file: " + listOfFiles[i].getName()).forEach(log::error);
    }
}
