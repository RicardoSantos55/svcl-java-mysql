package com.svcl.app.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class ParsedWorkbookData {
    private final Path sourcePath;
    private final List<CoverageRecord> coverageRecords;
    private final Map<String, Integer> distances;
    private final int totalPostalCodes;
    private final int totalCoverageRecords;
    private final int duplicatePostalCodes;
    private final int duplicateRows;
    private final int totalRoutes;

    public ParsedWorkbookData(
        Path sourcePath,
        List<CoverageRecord> coverageRecords,
        Map<String, Integer> distances,
        int totalPostalCodes,
        int totalCoverageRecords,
        int duplicatePostalCodes,
        int duplicateRows,
        int totalRoutes
    ) {
        this.sourcePath = sourcePath;
        this.coverageRecords = coverageRecords;
        this.distances = distances;
        this.totalPostalCodes = totalPostalCodes;
        this.totalCoverageRecords = totalCoverageRecords;
        this.duplicatePostalCodes = duplicatePostalCodes;
        this.duplicateRows = duplicateRows;
        this.totalRoutes = totalRoutes;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public List<CoverageRecord> getCoverageRecords() {
        return coverageRecords;
    }

    public Map<String, Integer> getDistances() {
        return distances;
    }

    public int getTotalPostalCodes() {
        return totalPostalCodes;
    }

    public int getTotalCoverageRecords() {
        return totalCoverageRecords;
    }

    public int getDuplicatePostalCodes() {
        return duplicatePostalCodes;
    }

    public int getDuplicateRows() {
        return duplicateRows;
    }

    public int getTotalRoutes() {
        return totalRoutes;
    }
}
