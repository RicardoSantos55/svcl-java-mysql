package com.svcl.app.data;

import com.svcl.app.model.CoverageRecord;
import com.svcl.app.model.CoverageResult;
import com.svcl.app.model.DatabaseSummary;
import com.svcl.app.model.ParsedWorkbookData;
import com.svcl.app.model.SearchOutcome;
import com.svcl.app.util.PostalCodeUtils;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CoverageRepository {
    private final String jdbcUrl;
    private final String databaseName;
    private final String username;
    private final String password;

    public CoverageRepository(String jdbcUrl, String databaseName, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
    }

    public synchronized void initializeSchema() throws SQLException {
        try (Connection connection = connect()) {
            initializeSchema(connection);
        }
    }

    public synchronized boolean hasData() throws SQLException {
        try (Connection connection = connect()) {
            initializeSchema(connection);
            return count(connection, "SELECT COUNT(*) FROM coverage") > 0;
        }
    }

    public synchronized DatabaseSummary importFromExcel(Path excelPath) throws Exception {
        ParsedWorkbookData parsed = WorkbookImporter.fromExcel(excelPath);
        try (Connection connection = connect()) {
            initializeSchema(connection);
            connection.setAutoCommit(false);
            try {
                execute(connection, "DELETE FROM coverage");
                execute(connection, "DELETE FROM distances");
                execute(connection, "DELETE FROM metadata");
                insertCoverageRecords(connection, parsed.getCoverageRecords());
                insertDistanceRows(connection, parsed.getDistances());

                Map<String, String> metadata = new LinkedHashMap<String, String>();
                metadata.put("source_name", parsed.getSourcePath().getFileName().toString());
                metadata.put("source_path", parsed.getSourcePath().toAbsolutePath().toString());
                metadata.put("total_postal_codes", String.valueOf(parsed.getTotalPostalCodes()));
                metadata.put("total_coverage_records", String.valueOf(parsed.getTotalCoverageRecords()));
                metadata.put("duplicate_postal_codes", String.valueOf(parsed.getDuplicatePostalCodes()));
                metadata.put("duplicate_rows", String.valueOf(parsed.getDuplicateRows()));
                metadata.put("total_routes", String.valueOf(parsed.getTotalRoutes()));
                replaceMetadata(connection, metadata);
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                throw error;
            }
        }
        return getSummary();
    }

    public synchronized int addManualRecord(Map<String, String> payload) throws SQLException {
        CoverageRecord record = normalizeManualRecord(payload);
        try (Connection connection = connect()) {
            initializeSchema(connection);
            int duplicateCount = countByPostalCode(connection, record.getPostalCode());
            insertCoverageRecord(connection, record);
            refreshMetadata(connection);
            return duplicateCount;
        }
    }

    public synchronized DatabaseSummary getSummary() throws SQLException {
        try (Connection connection = connect()) {
            initializeSchema(connection);
            Map<String, String> metadata = metadata(connection);
            if (count(connection, "SELECT COUNT(*) FROM coverage") > 0 && metadata.isEmpty()) {
                refreshMetadata(connection);
                metadata = metadata(connection);
            }
            return new DatabaseSummary(
                databaseName,
                jdbcUrl,
                emptyIfNull(metadata.get("source_name"), databaseName),
                emptyIfNull(metadata.get("source_path"), jdbcUrl),
                intValue(metadata, "total_postal_codes"),
                intValue(metadata, "total_coverage_records"),
                intValue(metadata, "duplicate_postal_codes"),
                intValue(metadata, "duplicate_rows"),
                intValue(metadata, "total_routes")
            );
        }
    }

    public synchronized SearchOutcome search(String originPlaza, String postalCode) throws SQLException {
        String normalizedPostalCode = PostalCodeUtils.normalizePostalCode(postalCode);
        List<CoverageRecord> rows = new ArrayList<CoverageRecord>();
        try (Connection connection = connect()) {
            initializeSchema(connection);
            try (
                PreparedStatement statement = connection.prepareStatement(
                    "SELECT postal_code, branch, plaza, municipality, city, neighborhood, state, coverage "
                        + "FROM coverage WHERE postal_code = ? ORDER BY plaza, neighborhood, city"
                )
            ) {
                statement.setString(1, normalizedPostalCode);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        rows.add(
                            new CoverageRecord(
                                resultSet.getString("postal_code"),
                                resultSet.getString("branch"),
                                resultSet.getString("plaza"),
                                resultSet.getString("municipality"),
                                resultSet.getString("city"),
                                resultSet.getString("neighborhood"),
                                resultSet.getString("state"),
                                resultSet.getString("coverage")
                            )
                        );
                    }
                }
            }

            List<CoverageResult> results = new ArrayList<CoverageResult>();
            Set<CoverageRecord> seen = new HashSet<CoverageRecord>();
            for (CoverageRecord row : rows) {
                if (!seen.add(row)) {
                    continue;
                }
                Integer distanceKm = getDistance(connection, originPlaza, row.getPlaza());
                results.add(
                    new CoverageResult(
                        row.getPostalCode(),
                        row.getState(),
                        row.getCity(),
                        row.getMunicipality(),
                        row.getNeighborhood(),
                        row.getCoverage(),
                        row.getBranch(),
                        row.getPlaza(),
                        distanceKm
                    )
                );
            }

            Collections.sort(
                results,
                Comparator
                    .comparing((CoverageResult item) -> item.getDistanceKm() == null)
                    .thenComparing(item -> item.getDistanceKm() == null ? Integer.valueOf(999999) : item.getDistanceKm())
                    .thenComparing(CoverageResult::getDestinationPlaza)
                    .thenComparing(CoverageResult::getNeighborhood)
            );

            return new SearchOutcome(normalizedPostalCode, rows.size(), rows.size() > 1, results);
        }
    }

    private Integer getDistance(Connection connection, String origin, String destination) throws SQLException {
        if (origin.equals(destination)) {
            return Integer.valueOf(0);
        }
        try (
            PreparedStatement statement = connection.prepareStatement(
                "SELECT distance_km FROM distances "
                    + "WHERE (origin = ? AND destination = ?) OR (origin = ? AND destination = ?) LIMIT 1"
            )
        ) {
            statement.setString(1, origin);
            statement.setString(2, destination);
            statement.setString(3, destination);
            statement.setString(4, origin);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return Integer.valueOf(resultSet.getInt("distance_km"));
            }
        }
    }

    private CoverageRecord normalizeManualRecord(Map<String, String> payload) {
        String postalCode = PostalCodeUtils.normalizePostalCode(value(payload, "postal_code"));
        Map<String, String> normalized = new HashMap<String, String>();
        normalized.put("branch", value(payload, "branch").toUpperCase());
        normalized.put("plaza", value(payload, "plaza").toUpperCase());
        normalized.put("municipality", value(payload, "municipality"));
        normalized.put("city", value(payload, "city"));
        normalized.put("neighborhood", value(payload, "neighborhood"));
        normalized.put("state", value(payload, "state"));
        normalized.put("coverage", value(payload, "coverage"));

        Map<String, String> labels = new LinkedHashMap<String, String>();
        labels.put("branch", "Sucursal destino");
        labels.put("plaza", "Plaza destino");
        labels.put("municipality", "Municipio");
        labels.put("city", "Ciudad");
        labels.put("neighborhood", "Colonia");
        labels.put("state", "Estado");
        labels.put("coverage", "Cobertura");

        for (Map.Entry<String, String> entry : labels.entrySet()) {
            if (normalized.get(entry.getKey()).isEmpty()) {
                throw new IllegalArgumentException("El campo '" + entry.getValue() + "' es obligatorio.");
            }
        }

        return new CoverageRecord(
            postalCode,
            normalized.get("branch"),
            normalized.get("plaza"),
            normalized.get("municipality"),
            normalized.get("city"),
            normalized.get("neighborhood"),
            normalized.get("state"),
            normalized.get("coverage")
        );
    }

    private void refreshMetadata(Connection connection) throws SQLException {
        Map<String, String> metadata = metadata(connection);
        String sourceName = emptyIfNull(metadata.get("source_name"), databaseName);
        String sourcePath = emptyIfNull(metadata.get("source_path"), jdbcUrl);

        Map<String, String> refreshed = new LinkedHashMap<String, String>();
        refreshed.put("source_name", sourceName);
        refreshed.put("source_path", sourcePath);
        refreshed.put("total_postal_codes", String.valueOf(count(connection, "SELECT COUNT(DISTINCT postal_code) FROM coverage")));
        refreshed.put("total_coverage_records", String.valueOf(count(connection, "SELECT COUNT(*) FROM coverage")));
        refreshed.put("total_routes", String.valueOf(count(connection, "SELECT COUNT(*) FROM distances")));

        try (
            Statement statement = connection.createStatement();
            ResultSet resultSet =
                statement.executeQuery(
                    "SELECT COUNT(*) AS duplicate_postal_codes, COALESCE(SUM(total - 1), 0) AS duplicate_rows "
                        + "FROM (SELECT postal_code, COUNT(*) AS total FROM coverage GROUP BY postal_code HAVING COUNT(*) > 1) duplicates"
                )
        ) {
            if (resultSet.next()) {
                refreshed.put("duplicate_postal_codes", String.valueOf(resultSet.getInt("duplicate_postal_codes")));
                refreshed.put("duplicate_rows", String.valueOf(resultSet.getInt("duplicate_rows")));
            } else {
                refreshed.put("duplicate_postal_codes", "0");
                refreshed.put("duplicate_rows", "0");
            }
        }

        replaceMetadata(connection, refreshed);
    }

    private void initializeSchema(Connection connection) throws SQLException {
        execute(
            connection,
            "CREATE TABLE IF NOT EXISTS metadata ("
                + "metadata_key VARCHAR(100) NOT NULL PRIMARY KEY, "
                + "metadata_value TEXT NOT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
        execute(
            connection,
            "CREATE TABLE IF NOT EXISTS coverage ("
                + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "postal_code VARCHAR(5) NOT NULL, "
                + "branch VARCHAR(32) NOT NULL, "
                + "plaza VARCHAR(16) NOT NULL, "
                + "municipality VARCHAR(255) NOT NULL, "
                + "city VARCHAR(255) NOT NULL, "
                + "neighborhood VARCHAR(255) NOT NULL, "
                + "state VARCHAR(255) NOT NULL, "
                + "coverage VARCHAR(255) NOT NULL, "
                + "INDEX idx_coverage_postal_code (postal_code), "
                + "INDEX idx_coverage_plaza (plaza)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
        execute(
            connection,
            "CREATE TABLE IF NOT EXISTS distances ("
                + "origin VARCHAR(16) NOT NULL, "
                + "destination VARCHAR(16) NOT NULL, "
                + "distance_km INT NOT NULL, "
                + "PRIMARY KEY (origin, destination)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    }

    private void insertCoverageRecords(Connection connection, List<CoverageRecord> records) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO coverage (postal_code, branch, plaza, municipality, city, neighborhood, state, coverage) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            )
        ) {
            for (CoverageRecord record : records) {
                statement.setString(1, record.getPostalCode());
                statement.setString(2, record.getBranch());
                statement.setString(3, record.getPlaza());
                statement.setString(4, record.getMunicipality());
                statement.setString(5, record.getCity());
                statement.setString(6, record.getNeighborhood());
                statement.setString(7, record.getState());
                statement.setString(8, record.getCoverage());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertDistanceRows(Connection connection, Map<String, Integer> distances) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO distances (origin, destination, distance_km) VALUES (?, ?, ?)"
            )
        ) {
            for (Map.Entry<String, Integer> entry : distances.entrySet()) {
                String[] route = entry.getKey().split("\\|\\|", 2);
                if (route.length != 2) {
                    continue;
                }
                statement.setString(1, route[0]);
                statement.setString(2, route[1]);
                statement.setInt(3, entry.getValue().intValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertCoverageRecord(Connection connection, CoverageRecord record) throws SQLException {
        try (
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO coverage (postal_code, branch, plaza, municipality, city, neighborhood, state, coverage) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            )
        ) {
            statement.setString(1, record.getPostalCode());
            statement.setString(2, record.getBranch());
            statement.setString(3, record.getPlaza());
            statement.setString(4, record.getMunicipality());
            statement.setString(5, record.getCity());
            statement.setString(6, record.getNeighborhood());
            statement.setString(7, record.getState());
            statement.setString(8, record.getCoverage());
            statement.executeUpdate();
        }
    }

    private int countByPostalCode(Connection connection, String postalCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM coverage WHERE postal_code = ?")) {
            statement.setString(1, postalCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private Map<String, String> metadata(Connection connection) throws SQLException {
        Map<String, String> metadata = new LinkedHashMap<String, String>();
        try (
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT metadata_key, metadata_value FROM metadata")
        ) {
            while (resultSet.next()) {
                metadata.put(resultSet.getString("metadata_key"), resultSet.getString("metadata_value"));
            }
        }
        return metadata;
    }

    private void replaceMetadata(Connection connection, Map<String, String> metadata) throws SQLException {
        execute(connection, "DELETE FROM metadata");
        try (
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO metadata (metadata_key, metadata_value) VALUES (?, ?)"
            )
        ) {
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                statement.setString(1, entry.getKey());
                statement.setString(2, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private int count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private static String value(Map<String, String> payload, String key) {
        String value = payload.get(key);
        return value == null ? "" : value.trim();
    }

    private static int intValue(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String emptyIfNull(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
