package com.svcl.app;

import com.svcl.app.model.BranchOffice;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public final class AppConfig {
    public static final Path APP_DIR = Paths.get("").toAbsolutePath().normalize();
    public static final Path DATA_DIR = APP_DIR.resolve("data");
    public static final Path STATIC_DIR = APP_DIR.resolve("src").resolve("main").resolve("resources").resolve("static");
    public static final Path IMPORT_FILE = DATA_DIR.resolve("current_database.xlsx");
    public static final String HOST = getenv("APP_HOST", "127.0.0.1");
    public static final int PORT = getenvInt("APP_PORT", 8010);
    public static final String AUTH_USERNAME = getenv("APP_USERNAME", "admin");
    public static final String AUTH_PASSWORD = getenv("APP_PASSWORD", "admin");
    public static final String SESSION_COOKIE_NAME = "coverage_session";
    public static final String DB_HOST = getenv("SVCL_DB_HOST", "127.0.0.1");
    public static final int DB_PORT = getenvInt("SVCL_DB_PORT", 3306);
    public static final String DB_NAME = getenv("SVCL_DB_NAME", "svcl");
    public static final String DB_USER = getenv("SVCL_DB_USER", "root");
    public static final String DB_PASSWORD = getenv("SVCL_DB_PASSWORD", "");
    public static final String DB_URL = getenv("SVCL_DB_URL", defaultJdbcUrl());

    public static final List<BranchOffice> BRANCH_OFFICES = Arrays.asList(
        new BranchOffice("Guadalajara", "Guadalajara", "Jalisco", "GDL02", "GDL", "44940"),
        new BranchOffice("Culiacan", "Culiacan", "Sinaloa", "CUL01", "CUL", "80220"),
        new BranchOffice("Los Mochis", "Los Mochis", "Sinaloa", "LMM01", "LMM", "81200"),
        new BranchOffice("Monterrey", "Monterrey", "Nuevo Leon", "MTY02", "MTY", "64536")
    );

    private AppConfig() {
    }

    private static String getenv(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static int getenvInt(String key, int fallback) {
        try {
            return Integer.parseInt(getenv(key, String.valueOf(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String defaultJdbcUrl() {
        return "jdbc:mysql://"
            + DB_HOST
            + ":"
            + DB_PORT
            + "/"
            + DB_NAME
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
    }
}
