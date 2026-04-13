package com.svcl.app.model;

import java.io.Serializable;

public final class CoverageResult implements Serializable {
    private final String postalCode;
    private final String state;
    private final String city;
    private final String municipality;
    private final String neighborhood;
    private final String coverage;
    private final String destinationBranch;
    private final String destinationPlaza;
    private final Integer distanceKm;

    public CoverageResult(
        String postalCode,
        String state,
        String city,
        String municipality,
        String neighborhood,
        String coverage,
        String destinationBranch,
        String destinationPlaza,
        Integer distanceKm
    ) {
        this.postalCode = postalCode;
        this.state = state;
        this.city = city;
        this.municipality = municipality;
        this.neighborhood = neighborhood;
        this.coverage = coverage;
        this.destinationBranch = destinationBranch;
        this.destinationPlaza = destinationPlaza;
        this.distanceKm = distanceKm;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getState() {
        return state;
    }

    public String getCity() {
        return city;
    }

    public String getMunicipality() {
        return municipality;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getCoverage() {
        return coverage;
    }

    public String getDestinationBranch() {
        return destinationBranch;
    }

    public String getDestinationPlaza() {
        return destinationPlaza;
    }

    public Integer getDistanceKm() {
        return distanceKm;
    }

    public boolean isWithinLimit() {
        return distanceKm != null && distanceKm <= 1600;
    }

    public String getDistanceLabel() {
        return distanceKm == null ? "Sin dato" : distanceKm + " km";
    }

    public String getLimitLabel() {
        if (distanceKm == null) {
            return "Sin distancia";
        }
        return isWithinLimit() ? "Si" : "No";
    }
}
