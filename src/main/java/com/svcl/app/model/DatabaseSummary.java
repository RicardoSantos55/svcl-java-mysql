package com.svcl.app.model;

import java.io.Serializable;

public final class DatabaseSummary implements Serializable {
    private final String databaseName;
    private final String databasePath;
    private final String sourceName;
    private final String sourcePath;
    private final int totalPostalCodes;
    private final int totalCoverageRecords;
    private final int duplicatePostalCodes;
    private final int duplicateRows;
    private final int totalRoutes;

    public DatabaseSummary(
        String databaseName,
        String databasePath,
        String sourceName,
        String sourcePath,
        int totalPostalCodes,
        int totalCoverageRecords,
        int duplicatePostalCodes,
        int duplicateRows,
        int totalRoutes
    ) {
        this.databaseName = databaseName;
        this.databasePath = databasePath;
        this.sourceName = sourceName;
        this.sourcePath = sourcePath;
        this.totalPostalCodes = totalPostalCodes;
        this.totalCoverageRecords = totalCoverageRecords;
        this.duplicatePostalCodes = duplicatePostalCodes;
        this.duplicateRows = duplicateRows;
        this.totalRoutes = totalRoutes;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourcePath() {
        return sourcePath;
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
