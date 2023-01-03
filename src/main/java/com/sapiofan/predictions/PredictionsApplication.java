package com.sapiofan.predictions;

import com.sapiofan.predictions.entities.Data;
import com.sapiofan.predictions.services.Statistics;
import com.sapiofan.predictions.services.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Timer;
import java.util.TimerTask;

@SpringBootApplication
public class PredictionsApplication {

    @Autowired
    private static Statistics statistics;

    public static void main(String[] args) {
        SpringApplication.run(PredictionsApplication.class, args);
        Data data = statistics.getWorldData();
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (String country : Utils.getCountries()) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            statistics.getCountryData(data, country);
                        }
                    });
                    thread.start();
                }
            }
        }, 0, 43200000);
    }

}
