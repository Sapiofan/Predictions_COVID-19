package com.sapiofan.predictions.dao;

import com.sapiofan.predictions.entities.db.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryDao extends JpaRepository<Country, Long> {

}
