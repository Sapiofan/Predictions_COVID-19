package com.sapiofan.predictions.dao;

import com.sapiofan.predictions.entities.db.Country;
import com.sapiofan.predictions.entities.db.CovidDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface CountryDao extends JpaRepository<Country, Long> {
    List<Country> getCountriesByDate(CovidDate covidDate);

    @Query("select c from Country c where c.date.date > :date and (c.country='World' or c.country='Europe' " +
            "or c.country='Asia' or c.country='Americas' or c.country='Oceania' or c.country='Africa')")
    List<Country> getRegionsForPeriod(LocalDate date);

    @Query("select c from Country c where c.date.date > :date and c.country=:country")
    List<Country> getCountry(LocalDate date, String country);
}
