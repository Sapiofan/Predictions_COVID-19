package com.sapiofan.predictions.entities.db;

import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "dates")
@NoArgsConstructor
public class CovidDate implements Comparable<CovidDate>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private LocalDate date;

    private Boolean isPredictionDate;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "date")
    private List<Country> countries = new ArrayList<>();

    public CovidDate(LocalDate date, Boolean isPredictionDate) {
        this.date = date;
        this.isPredictionDate = isPredictionDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Boolean getPredictionDate() {
        return isPredictionDate;
    }

    public void setPredictionDate(Boolean predictionDate) {
        isPredictionDate = predictionDate;
    }

    public List<Country> getCountries() {
        return countries;
    }

    public void setCountries(List<Country> countries) {
        this.countries = countries;
    }

    public void addCountry(Country country) {
        this.countries.add(country);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CovidDate covidDate = (CovidDate) o;
        return id.equals(covidDate.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(CovidDate o) {
        if(o == null) {
            return 0;
        }
        return date.compareTo(o.date);
    }
}
