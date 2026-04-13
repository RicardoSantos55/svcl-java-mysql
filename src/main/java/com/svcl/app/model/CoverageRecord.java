package com.svcl.app.model;

import java.io.Serializable;
import java.util.Objects;

public final class CoverageRecord implements Serializable {
    private final String postalCode;
    private final String branch;
    private final String plaza;
    private final String municipality;
    private final String city;
    private final String neighborhood;
    private final String state;
    private final String coverage;

    public CoverageRecord(
        String postalCode,
        String branch,
        String plaza,
        String municipality,
        String city,
        String neighborhood,
        String state,
        String coverage
    ) {
        this.postalCode = postalCode;
        this.branch = branch;
        this.plaza = plaza;
        this.municipality = municipality;
        this.city = city;
        this.neighborhood = neighborhood;
        this.state = state;
        this.coverage = coverage;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getBranch() {
        return branch;
    }

    public String getPlaza() {
        return plaza;
    }

    public String getMunicipality() {
        return municipality;
    }

    public String getCity() {
        return city;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getState() {
        return state;
    }

    public String getCoverage() {
        return coverage;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CoverageRecord)) {
            return false;
        }
        CoverageRecord record = (CoverageRecord) other;
        return Objects.equals(postalCode, record.postalCode)
            && Objects.equals(branch, record.branch)
            && Objects.equals(plaza, record.plaza)
            && Objects.equals(municipality, record.municipality)
            && Objects.equals(city, record.city)
            && Objects.equals(neighborhood, record.neighborhood)
            && Objects.equals(state, record.state)
            && Objects.equals(coverage, record.coverage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postalCode, branch, plaza, municipality, city, neighborhood, state, coverage);
    }
}
