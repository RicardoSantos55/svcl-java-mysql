package com.svcl.app.model;

import java.io.Serializable;
import java.util.List;

public final class SearchOutcome implements Serializable {
    private final String postalCode;
    private final int rawMatchCount;
    private final boolean duplicatePostalCode;
    private final List<CoverageResult> results;

    public SearchOutcome(String postalCode, int rawMatchCount, boolean duplicatePostalCode, List<CoverageResult> results) {
        this.postalCode = postalCode;
        this.rawMatchCount = rawMatchCount;
        this.duplicatePostalCode = duplicatePostalCode;
        this.results = results;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public int getRawMatchCount() {
        return rawMatchCount;
    }

    public boolean isDuplicatePostalCode() {
        return duplicatePostalCode;
    }

    public List<CoverageResult> getResults() {
        return results;
    }
}
