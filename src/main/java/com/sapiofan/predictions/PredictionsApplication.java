package com.sapiofan.predictions;

import com.sapiofan.predictions.entities.Data;
import com.sapiofan.predictions.services.impl.FileHandlerServiceImpl;
import com.sapiofan.predictions.services.Statistics;
import com.sapiofan.predictions.services.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@SpringBootApplication
public class PredictionsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PredictionsApplication.class, args);
    }
}
