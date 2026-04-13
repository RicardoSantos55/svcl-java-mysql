package com.svcl.app;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class MultipartParser {
    private MultipartParser() {
    }

    public static UploadPart extractFile(String contentType, byte[] body, String fieldName) {
        String boundary = boundary(contentType);
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        int cursor = 0;

        while (true) {
            int start = indexOf(body, boundaryBytes, cursor);
            if (start < 0) {
                break;
            }
            int partStart = start + boundaryBytes.length;
            if (partStart + 1 < body.length && body[partStart] == '-' && body[partStart + 1] == '-') {
                break;
            }
            if (partStart + 1 < body.length && body[partStart] == '\r' && body[partStart + 1] == '\n') {
                partStart += 2;
            }

            int nextBoundary = indexOf(body, boundaryBytes, partStart);
            if (nextBoundary < 0) {
                break;
            }

            int headersEnd = indexOf(body, "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1), partStart);
            if (headersEnd < 0 || headersEnd > nextBoundary) {
                cursor = nextBoundary;
                continue;
            }

            String headers = new String(body, partStart, headersEnd - partStart, StandardCharsets.ISO_8859_1);
            int bodyStart = headersEnd + 4;
            int bodyEnd = nextBoundary;
            if (bodyEnd - 2 >= bodyStart && body[bodyEnd - 2] == '\r' && body[bodyEnd - 1] == '\n') {
                bodyEnd -= 2;
            }

            if (headers.contains("name=\"" + fieldName + "\"")) {
                String filename = filename(headers);
                byte[] payload = Arrays.copyOfRange(body, bodyStart, bodyEnd);
                if (payload.length == 0) {
                    throw new IllegalArgumentException("El archivo enviado esta vacio.");
                }
                return new UploadPart(filename == null ? "database.xlsx" : filename, payload);
            }

            cursor = nextBoundary;
        }

        throw new IllegalArgumentException("No se encontro el archivo en la solicitud.");
    }

    private static String boundary(String contentType) {
        if (contentType == null || !contentType.contains("multipart/form-data")) {
            throw new IllegalArgumentException("La carga debe enviarse como multipart/form-data.");
        }
        String[] parts = contentType.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("boundary=")) {
                return trimmed.substring("boundary=".length()).replace("\"", "");
            }
        }
        throw new IllegalArgumentException("No se pudo determinar el boundary de la solicitud.");
    }

    private static String filename(String headers) {
        String marker = "filename=\"";
        int index = headers.indexOf(marker);
        if (index < 0) {
            return null;
        }
        int start = index + marker.length();
        int end = headers.indexOf('"', start);
        if (end < 0) {
            return null;
        }
        return headers.substring(start, end);
    }

    private static int indexOf(byte[] haystack, byte[] needle, int start) {
        for (int index = Math.max(start, 0); index <= haystack.length - needle.length; index++) {
            boolean matches = true;
            for (int offset = 0; offset < needle.length; offset++) {
                if (haystack[index + offset] != needle[offset]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return index;
            }
        }
        return -1;
    }

    public static final class UploadPart {
        private final String filename;
        private final byte[] payload;

        public UploadPart(String filename, byte[] payload) {
            this.filename = filename;
            this.payload = payload;
        }

        public String getFilename() {
            return filename;
        }

        public byte[] getPayload() {
            return payload;
        }
    }
}
