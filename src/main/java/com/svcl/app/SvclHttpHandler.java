package com.svcl.app;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.svcl.app.model.DatabaseSummary;
import com.svcl.app.util.JsonUtils;
import com.svcl.app.util.SimpleJsonParser;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SvclHttpHandler implements HttpHandler {
    private final AppState appState;
    private final SessionStore sessionStore;

    public SvclHttpHandler(AppState appState, SessionStore sessionStore) {
        this.appState = appState;
        this.sessionStore = sessionStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (IllegalArgumentException error) {
            sendJson(exchange, 400, errorPayload(error.getMessage()), null);
        } catch (Exception error) {
            sendJson(exchange, 500, errorPayload(error.getMessage()), null);
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        if ("GET".equalsIgnoreCase(method)) {
            if ("/api/session".equals(path)) {
                handleSession(exchange);
                return;
            }
            if ("/api/status".equals(path)) {
                if (!requireAuth(exchange)) {
                    return;
                }
                sendJson(exchange, 200, appState.statusPayload(), null);
                return;
            }
            serveStatic(exchange, path);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            if ("/api/login".equals(path)) {
                handleLogin(exchange);
                return;
            }
            if ("/api/logout".equals(path)) {
                handleLogout(exchange);
                return;
            }
            if ("/api/search".equals(path)) {
                if (!requireAuth(exchange)) {
                    return;
                }
                Map<String, String> payload = SimpleJsonParser.parseObject(readBody(exchange));
                sendJson(exchange, 200, appState.search(value(payload, "branch"), value(payload, "postalCode")), null);
                return;
            }
            if ("/api/import".equals(path)) {
                if (!requireAuth(exchange)) {
                    return;
                }
                MultipartParser.UploadPart uploadPart = MultipartParser.extractFile(
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestBody().readAllBytes(),
                    "database"
                );
                if (!uploadPart.getFilename().toLowerCase().endsWith(".xlsx")) {
                    throw new IllegalArgumentException("Solo se permiten archivos .xlsx.");
                }
                DatabaseSummary summary = appState.importData(uploadPart.getFilename(), uploadPart.getPayload());
                Map<String, Object> response = new LinkedHashMap<String, Object>();
                response.put("message", appState.statusPayload().get("lastNotice"));
                response.put("importSummary", summaryPayload(summary));
                response.put("status", appState.statusPayload());
                sendJson(exchange, 200, response, null);
                return;
            }
            if ("/api/manual-entry".equals(path)) {
                if (!requireAuth(exchange)) {
                    return;
                }
                Map<String, String> payload = SimpleJsonParser.parseObject(readBody(exchange));
                AppState.ResultWithMessage result = appState.addManualData(payload);
                Map<String, Object> response = new LinkedHashMap<String, Object>();
                response.put("message", result.getMessage());
                response.put("manualSummary", summaryPayload(result.getSummary()));
                response.put("status", appState.statusPayload());
                sendJson(exchange, 200, response, null);
                return;
            }
        }

        sendJson(exchange, 404, errorPayload("Ruta no encontrada."), null);
    }

    private void handleSession(HttpExchange exchange) throws IOException {
        String username = currentUser(exchange);
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("authenticated", Boolean.valueOf(username != null));
        payload.put("username", username == null ? "" : username);
        sendJson(exchange, 200, payload, null);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, String> payload = SimpleJsonParser.parseObject(readBody(exchange));
        String username = value(payload, "username");
        String password = value(payload, "password");
        if (!AppConfig.AUTH_USERNAME.equals(username) || !AppConfig.AUTH_PASSWORD.equals(password)) {
            sendJson(exchange, 401, errorPayload("Usuario o contrasena incorrectos."), null);
            return;
        }
        String token = sessionStore.create(username);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("authenticated", Boolean.TRUE);
        response.put("username", username);
        sendJson(
            exchange,
            200,
            response,
            AppConfig.SESSION_COOKIE_NAME + "=" + token + "; HttpOnly; Path=/; SameSite=Lax"
        );
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = sessionToken(exchange);
        sessionStore.destroy(token);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("authenticated", Boolean.FALSE);
        sendJson(
            exchange,
            200,
            response,
            AppConfig.SESSION_COOKIE_NAME + "=; HttpOnly; Path=/; Max-Age=0; SameSite=Lax"
        );
    }

    private boolean requireAuth(HttpExchange exchange) throws IOException {
        if (currentUser(exchange) != null) {
            return true;
        }
        sendJson(exchange, 401, errorPayload("Debes iniciar sesion para continuar."), null);
        return false;
    }

    private String currentUser(HttpExchange exchange) {
        return sessionStore.getUsername(sessionToken(exchange));
    }

    private String sessionToken(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) {
            return null;
        }
        for (String header : cookies) {
            for (HttpCookie cookie : HttpCookie.parse(header)) {
                if (AppConfig.SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void serveStatic(HttpExchange exchange, String routePath) throws IOException {
        String requested = routePath == null || routePath.isEmpty() || "/".equals(routePath) ? "index.html" : routePath.substring(1);
        Path staticFile = AppConfig.STATIC_DIR.resolve(requested).normalize();
        if (!staticFile.startsWith(AppConfig.STATIC_DIR) || !Files.exists(staticFile) || Files.isDirectory(staticFile)) {
            sendJson(exchange, 404, errorPayload("Archivo no encontrado."), null);
            return;
        }

        String contentType = guessContentType(staticFile);
        byte[] content = Files.readAllBytes(staticFile);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, content.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(content);
        outputStream.flush();
    }

    private void sendJson(HttpExchange exchange, int statusCode, Map<String, Object> payload, String setCookie) throws IOException {
        byte[] content = JsonUtils.toJson(payload).getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        if (setCookie != null) {
            headers.add("Set-Cookie", setCookie);
        }
        exchange.sendResponseHeaders(statusCode, content.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(content);
        outputStream.flush();
    }

    private Map<String, Object> errorPayload(String message) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("error", message);
        return payload;
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String guessContentType(Path file) {
        String filename = file.getFileName().toString().toLowerCase();
        if (filename.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (filename.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (filename.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        return "application/octet-stream";
    }

    private static String value(Map<String, String> payload, String key) {
        String value = payload.get(key);
        return value == null ? "" : value.trim();
    }

    private Map<String, Object> summaryPayload(DatabaseSummary summary) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("sourceName", summary.getSourceName());
        payload.put("sourcePath", summary.getSourcePath());
        payload.put("totalPostalCodes", Integer.valueOf(summary.getTotalPostalCodes()));
        payload.put("totalCoverageRecords", Integer.valueOf(summary.getTotalCoverageRecords()));
        payload.put("duplicatePostalCodes", Integer.valueOf(summary.getDuplicatePostalCodes()));
        payload.put("duplicateRows", Integer.valueOf(summary.getDuplicateRows()));
        payload.put("totalRoutes", Integer.valueOf(summary.getTotalRoutes()));
        return payload;
    }
}
