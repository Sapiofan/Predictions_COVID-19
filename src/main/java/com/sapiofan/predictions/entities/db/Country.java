package com.sapiofan.predictions.entities.db;

import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "countries")
@NoArgsConstructor
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private Integer new_cases;

    @Column(nullable = false)
    private Integer new_deaths;

    @Column(nullable = false)
    private Integer confirmed_cases;

    @Column(nullable = false)
    private Integer confirmed_deaths;

    private Integer low_bound_cases;

    private Integer high_bound_cases;

    private Integer low_bound_deaths;

    private Integer high_bound_deaths;

    @ManyToOne(fetch = FetchType.EAGER)
    private CovidDate date;

    public Country(String country, Integer new_cases, Integer new_deaths,
                   Integer confirmed_cases, Integer confirmed_deaths,
                   Integer low_bound_cases, Integer high_bound_cases,
                   Integer low_bound_deaths, Integer high_bound_deaths, CovidDate date) {
        this.country = country;
        this.new_cases = new_cases;
        this.new_deaths = new_deaths;
        this.confirmed_cases = confirmed_cases;
        this.confirmed_deaths = confirmed_deaths;
        this.low_bound_cases = low_bound_cases;
        this.high_bound_cases = high_bound_cases;
        this.low_bound_deaths = low_bound_deaths;
        this.high_bound_deaths = high_bound_deaths;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getNew_cases() {
        return new_cases;
    }

    public void setNew_cases(Integer new_cases) {
        this.new_cases = new_cases;
    }

    public Integer getNew_deaths() {
        return new_deaths;
    }

    public void setNew_deaths(Integer new_deaths) {
        this.new_deaths = new_deaths;
    }

    public Integer getConfirmed_cases() {
        return confirmed_cases;
    }

    public void setConfirmed_cases(Integer confirmed_cases) {
        this.confirmed_cases = confirmed_cases;
    }

    public Integer getConfirmed_deaths() {
        return confirmed_deaths;
    }

    public void setConfirmed_deaths(Integer confirmed_deaths) {
        this.confirmed_deaths = confirmed_deaths;
    }

    public Integer getLow_bound_cases() {
        return low_bound_cases;
    }

    public void setLow_bound_cases(Integer low_bound_cases) {
        this.low_bound_cases = low_bound_cases;
    }

    public Integer getHigh_bound_cases() {
        return high_bound_cases;
    }

    public void setHigh_bound_cases(Integer high_bound_cases) {
        this.high_bound_cases = high_bound_cases;
    }

    public Integer getLow_bound_deaths() {
        return low_bound_deaths;
    }

    public void setLow_bound_deaths(Integer low_bound_deaths) {
        this.low_bound_deaths = low_bound_deaths;
    }

    public Integer getHigh_bound_deaths() {
        return high_bound_deaths;
    }

    public void setHigh_bound_deaths(Integer high_bound_deaths) {
        this.high_bound_deaths = high_bound_deaths;
    }

    public CovidDate getDate() {
        return date;
    }

    public void setDate(CovidDate date) {
        this.date = date;
        this.date.addCountry(this);
    }
}
