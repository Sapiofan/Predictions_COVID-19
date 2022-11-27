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

@Service
public class Statistics {

    private static final Logger log = LoggerFactory.getLogger(Statistics.class);

    public void getWorldStatistics() {
        downloadFilesForLastYear();
    }

    private void downloadFilesForLastYear() {
        String urlString = "https://raw.githubusercontent.com/CSSEGISandData/" +
                "COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports/";
        String temp = urlString;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        String today = LocalDate.now().minusDays(1L).format(formatter);
        log.info(today);

        Path folder = Paths.get("src/main/resources/statistics/");
        File file = new File(folder.toString());

        for (int i = 0; i < 3; i++) {
            temp += today + ".csv";
            try {
                downloadFile(temp, folder + "/" + today + ".csv");
            } catch (IOException e) {
                e.printStackTrace();
            }
            today = LocalDate.parse(today, formatter).minusDays(1L).format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
            temp = urlString;
        }
    }

    private void downloadFile(String urlStr, String file) throws IOException {
        URL url = new URL(urlStr);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
        rbc.close();
    }
}
