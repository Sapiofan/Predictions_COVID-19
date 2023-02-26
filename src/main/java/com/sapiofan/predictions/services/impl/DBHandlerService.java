package com.sapiofan.predictions.services.impl;

import com.sapiofan.predictions.dao.DateDao;
import com.sapiofan.predictions.entities.Data;
import com.sapiofan.predictions.entities.db.CovidDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DBHandlerService {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired
    private DateDao dateDao;

    public void updateData(Data data) {
        List<CovidDate> dates = dateDao.findAll();
        LocalDate periodAgo = LocalDate.now().minusDays(15);

        for (Map.Entry<String, Map<String, Long>> stringMapEntry : data.getConfirmedCases().entrySet()) {
            LocalDate date = LocalDate.parse(stringMapEntry.getKey()
                    .substring(stringMapEntry.getKey().indexOf(".")), formatter);
            LocalDate localDate = dates.stream().filter(covidDate -> covidDate.getDate().equals(date))
                    .collect(Collectors.toList()).get(0).getDate();
            if(localDate.equals(date)) {
                continue;
            }
            if(localDate.isAfter(date)) {

            }
        }
    }

    public Data loadData() {
        Data data = new Data();

        return data;
    }
}
