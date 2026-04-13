package com.svcl.app.model;

import java.io.Serializable;

public final class BranchOffice implements Serializable {
    private final String label;
    private final String city;
    private final String state;
    private final String sucursal;
    private final String plaza;
    private final String postalCode;

    public BranchOffice(
        String label,
        String city,
        String state,
        String sucursal,
        String plaza,
        String postalCode
    ) {
        this.label = label;
        this.city = city;
        this.state = state;
        this.sucursal = sucursal;
        this.plaza = plaza;
        this.postalCode = postalCode;
    }

    public String getLabel() {
        return label;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getSucursal() {
        return sucursal;
    }

    public String getPlaza() {
        return plaza;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getDescription() {
        return city + ", " + state + " | " + sucursal + "/" + plaza + " | CP " + postalCode;
    }
}
