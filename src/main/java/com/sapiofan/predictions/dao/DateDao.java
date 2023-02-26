package com.sapiofan.predictions.dao;

import com.sapiofan.predictions.entities.db.CovidDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface DateDao extends JpaRepository<CovidDate, Long> {
    @Query("select c from CovidDate c")
    Set<CovidDate> getAllCovidDates();
}
