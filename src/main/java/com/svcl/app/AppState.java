package com.svcl.app;

import com.svcl.app.data.CoverageRepository;
import com.svcl.app.model.BranchOffice;
import com.svcl.app.model.CoverageResult;
import com.svcl.app.model.DatabaseSummary;
import com.svcl.app.model.SearchOutcome;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AppState {
    private CoverageRepository repository;
    private String lastError = "";
    private String lastNotice = "";

    public synchronized void initialize() throws Exception {
        Files.createDirectories(AppConfig.DATA_DIR);
        repository = createRepository();
        try {
            repository.initializeSchema();
            if (repository.hasData()) {
                lastNotice = "Base MySQL cargada correctamente.";
                lastError = "";
                return;
            }
            if (Files.exists(AppConfig.IMPORT_FILE)) {
                DatabaseSummary summary = repository.importFromExcel(AppConfig.IMPORT_FILE);
                lastNotice = importNotice(summary);
                lastError = "";
                return;
            }
            lastNotice = "Conexion MySQL lista. Importa un archivo Excel o agrega registros manuales.";
            lastError = "";
        } catch (Exception error) {
            lastError = "No fue posible conectar a MySQL: " + error.getMessage();
            lastNotice = "";
        }
    }

    public synchronized DatabaseSummary importData(String filename, byte[] payload) throws Exception {
        Files.createDirectories(AppConfig.DATA_DIR);
        CoverageRepository activeRepository = ensureRepository();
        Path temporaryFile = Files.createTempFile(AppConfig.DATA_DIR, "svcl-upload-", suffix(filename));
        Files.write(temporaryFile, payload);
        try {
            DatabaseSummary summary = activeRepository.importFromExcel(temporaryFile);
            Files.write(AppConfig.IMPORT_FILE, payload);
            lastNotice = importNotice(summary);
            lastError = "";
            return summary;
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    public synchronized ResultWithMessage addManualData(Map<String, String> payload) throws Exception {
        CoverageRepository activeRepository = ensureRepository();
        int duplicateCount = activeRepository.addManualRecord(payload);
        DatabaseSummary summary = activeRepository.getSummary();
        lastError = "";
        if (duplicateCount > 0) {
            lastNotice =
                "Registro agregado. El CP "
                    + value(payload, "postal_code")
                    + " ya existia "
                    + duplicateCount
                    + " vez/veces en la base.";
        } else {
            lastNotice = "Registro agregado correctamente a la base de datos.";
        }
        return new ResultWithMessage(summary, lastNotice);
    }

    public synchronized Map<String, Object> statusPayload() {
        DatabaseSummary summary = null;
        try {
            summary = summary();
        } catch (Exception error) {
            lastError = "No fue posible consultar MySQL: " + error.getMessage();
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("databaseLoaded", Boolean.valueOf(summary != null && summary.getTotalCoverageRecords() > 0));
        payload.put("databaseName", summary != null ? summary.getDatabaseName() : AppConfig.DB_NAME);
        payload.put("databasePath", summary != null ? summary.getDatabasePath() : AppConfig.DB_URL);
        payload.put("sourceName", summary != null ? summary.getSourceName() : "");
        payload.put("sourcePath", summary != null ? summary.getSourcePath() : "");
        payload.put("totalPostalCodes", Integer.valueOf(summary != null ? summary.getTotalPostalCodes() : 0));
        payload.put("totalCoverageRecords", Integer.valueOf(summary != null ? summary.getTotalCoverageRecords() : 0));
        payload.put("duplicatePostalCodes", Integer.valueOf(summary != null ? summary.getDuplicatePostalCodes() : 0));
        payload.put("duplicateRows", Integer.valueOf(summary != null ? summary.getDuplicateRows() : 0));
        payload.put("totalRoutes", Integer.valueOf(summary != null ? summary.getTotalRoutes() : 0));
        payload.put("lastError", lastError);
        payload.put("lastNotice", lastNotice);

        List<Map<String, Object>> branches = new ArrayList<Map<String, Object>>();
        for (BranchOffice branch : AppConfig.BRANCH_OFFICES) {
            Map<String, Object> branchPayload = new LinkedHashMap<String, Object>();
            branchPayload.put("label", branch.getLabel());
            branchPayload.put("description", branch.getDescription());
            branchPayload.put("city", branch.getCity());
            branchPayload.put("state", branch.getState());
            branchPayload.put("sucursal", branch.getSucursal());
            branchPayload.put("plaza", branch.getPlaza());
            branchPayload.put("postalCode", branch.getPostalCode());
            branches.add(branchPayload);
        }
        payload.put("branches", branches);
        return payload;
    }

    public synchronized Map<String, Object> search(String branchLabel, String postalCode) throws Exception {
        CoverageRepository activeRepository = ensureRepository();
        if (!activeRepository.hasData()) {
            throw new IllegalArgumentException("No hay una base de datos cargada.");
        }
        BranchOffice selectedBranch = null;
        for (BranchOffice branch : AppConfig.BRANCH_OFFICES) {
            if (branch.getLabel().equals(branchLabel)) {
                selectedBranch = branch;
                break;
            }
        }
        if (selectedBranch == null) {
            throw new IllegalArgumentException("La sucursal seleccionada no es valida.");
        }

        SearchOutcome outcome = activeRepository.search(selectedBranch.getPlaza(), postalCode);
        int withinLimit = 0;
        for (CoverageResult result : outcome.getResults()) {
            if (result.isWithinLimit()) {
                withinLimit++;
            }
        }

        String duplicateNotice = "";
        if (outcome.isDuplicatePostalCode()) {
            duplicateNotice =
                "Aviso: el codigo postal "
                    + outcome.getPostalCode()
                    + " aparece "
                    + outcome.getRawMatchCount()
                    + " veces en la base de datos.";
        }

        String summaryText;
        if (!outcome.getResults().isEmpty()) {
            summaryText =
                "Sucursal origen: "
                    + selectedBranch.getSucursal()
                    + "/"
                    + selectedBranch.getPlaza()
                    + ". Coincidencias: "
                    + outcome.getResults().size()
                    + ". Dentro del limite de 1600 km: "
                    + withinLimit
                    + ".";
        } else {
            summaryText =
                "El codigo postal " + outcome.getPostalCode() + " no tiene cobertura registrada en la base actual.";
        }

        Map<String, Object> branchPayload = new LinkedHashMap<String, Object>();
        branchPayload.put("label", selectedBranch.getLabel());
        branchPayload.put("description", selectedBranch.getDescription());
        branchPayload.put("sucursal", selectedBranch.getSucursal());
        branchPayload.put("plaza", selectedBranch.getPlaza());

        List<Map<String, Object>> resultPayload = new ArrayList<Map<String, Object>>();
        for (CoverageResult result : outcome.getResults()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("postal_code", result.getPostalCode());
            item.put("state", result.getState());
            item.put("city", result.getCity());
            item.put("municipality", result.getMunicipality());
            item.put("neighborhood", result.getNeighborhood());
            item.put("coverage", result.getCoverage());
            item.put("destination_branch", result.getDestinationBranch());
            item.put("destination_plaza", result.getDestinationPlaza());
            item.put("distance_km", result.getDistanceKm());
            item.put("distanceLabel", result.getDistanceLabel());
            item.put("limitLabel", result.getLimitLabel());
            item.put("withinLimit", Boolean.valueOf(result.isWithinLimit()));
            resultPayload.add(item);
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("branch", branchPayload);
        payload.put("postalCode", outcome.getPostalCode());
        payload.put("summary", summaryText);
        payload.put("duplicateNotice", duplicateNotice);
        payload.put("duplicatePostalCode", Boolean.valueOf(outcome.isDuplicatePostalCode()));
        payload.put("rawMatchCount", Integer.valueOf(outcome.getRawMatchCount()));
        payload.put("results", resultPayload);
        return payload;
    }

    private DatabaseSummary summary() throws Exception {
        return repository != null && repository.hasData() ? repository.getSummary() : null;
    }

    private static String importNotice(DatabaseSummary summary) {
        if (summary.getDuplicatePostalCodes() > 0) {
            return "Datos importados en MySQL. Se detectaron "
                + summary.getDuplicatePostalCodes()
                + " codigos postales repetidos y "
                + summary.getDuplicateRows()
                + " filas adicionales con el mismo CP.";
        }
        return "Datos importados en MySQL sin codigos postales repetidos.";
    }

    private static String value(Map<String, String> payload, String key) {
        String value = payload.get(key);
        return value == null ? "" : value.trim();
    }

    private static String suffix(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return ".xlsx";
        }
        return filename.substring(dotIndex);
    }

    private CoverageRepository createRepository() {
        return new CoverageRepository(AppConfig.DB_URL, AppConfig.DB_NAME, AppConfig.DB_USER, AppConfig.DB_PASSWORD);
    }

    private CoverageRepository ensureRepository() throws Exception {
        if (repository == null) {
            repository = createRepository();
        }
        repository.initializeSchema();
        return repository;
    }

    public static final class ResultWithMessage {
        private final DatabaseSummary summary;
        private final String message;

        public ResultWithMessage(DatabaseSummary summary, String message) {
            this.summary = summary;
            this.message = message;
        }

        public DatabaseSummary getSummary() {
            return summary;
        }

        public String getMessage() {
            return message;
        }
    }
}
