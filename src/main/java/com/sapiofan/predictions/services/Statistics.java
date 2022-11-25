package com.sapiofan.predictions.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
public class Statistics {

    private static final Logger log = LoggerFactory.getLogger(Statistics.class);

    public void getWorldStatistics() {
        downloadFilesForLastYear();
    }

    private void downloadFilesForLastYear() {
        String urlString = "https://github.com/CSSEGISandData/COVID-19/tree/master/csse_covid_19_data/csse_covid_19_daily_reports/";
        String today = LocalDate.now().minusDays(1l).format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
        log.info(today);

        Path folder = Paths.get(this.getClass().getResource("/").getPath());
        Path newFolder = Paths.get(folder.toAbsolutePath() + "/statistics/");
        Path path = null;
        try {
            path = Files.createDirectories(newFolder);
        } catch (IOException e) {
            log.error("Can't create directory");
            return;
        }
        File file = new File(path.toString());

        for (int i = 0; i < 365; i++) {
            urlString += today + ".csv";
            URL url;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                log.error("Can't read file by link: " + urlString);
                return;
            }
            copyURLToFile(url, file);
            today = LocalDate.parse(today).minusDays(1l).format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
        }
    }

    private void copyURLToFile(URL url, File file) {

        try {
            InputStream input = url.openStream();
            if (file.exists()) {
                if (file.isDirectory())
                    throw new IOException("File '" + file + "' is a directory");

                if (!file.canWrite())
                    throw new IOException("File '" + file + "' cannot be written");
            } else {
                File parent = file.getParentFile();
                if ((parent != null) && (!parent.exists()) && (!parent.mkdirs())) {
                    throw new IOException("File '" + file + "' could not be created");
                }
            }

            FileOutputStream output = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }

            input.close();
            output.close();

            System.out.println("File '" + file + "' downloaded successfully!");
        }
        catch(IOException ioEx) {
            ioEx.printStackTrace();
        }
    }
}
