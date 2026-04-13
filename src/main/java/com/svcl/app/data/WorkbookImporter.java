package com.svcl.app.data;

import com.svcl.app.model.CoverageRecord;
import com.svcl.app.model.ParsedWorkbookData;
import com.svcl.app.util.PostalCodeUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class WorkbookImporter {
    private static final String COVERAGE_SHEET = "COBERTURA TOTAL";
    private static final String DISTANCE_SHEET = "DISTANCIA ENTRE SUCURSALES";

    private WorkbookImporter() {
    }

    public static ParsedWorkbookData fromExcel(Path excelPath) throws Exception {
        if (!Files.exists(excelPath)) {
            throw new IOException("No se encontro el archivo: " + excelPath.toAbsolutePath());
        }
        byte[] content = Files.readAllBytes(excelPath);
        XlsxWorkbook workbook = new XlsxWorkbook(content);
        List<List<String>> coverageRows = workbook.readSheet(COVERAGE_SHEET);
        List<List<String>> distanceRows = workbook.readSheet(DISTANCE_SHEET);

        if (coverageRows.isEmpty()) {
            throw new IllegalArgumentException("La hoja '" + COVERAGE_SHEET + "' esta vacia.");
        }
        if (distanceRows.isEmpty()) {
            throw new IllegalArgumentException("La hoja '" + DISTANCE_SHEET + "' esta vacia.");
        }

        List<String> coverageHeaders = coverageRows.get(0);
        List<String> distanceHeaders = distanceRows.get(0);

        Map<String, String> normalizedCoverageHeaders = normalizeHeaders(coverageHeaders);
        Map<String, String> normalizedDistanceHeaders = normalizeHeaders(distanceHeaders);

        requireColumns(
            COVERAGE_SHEET,
            normalizedCoverageHeaders,
            new String[] {
                "CODIGO POSTAL",
                "SUCURSAL",
                "PLAZA",
                "DELEGACION/MUNICIPIO",
                "CIUDAD",
                "COLONIA/ASENTAMIENTO",
                "ESTADO",
                "COBERTURA"
            }
        );
        requireColumns(DISTANCE_SHEET, normalizedDistanceHeaders, new String[] {"ORIGEN", "DESTINO"});

        String distanceColumnName = null;
        if (normalizedDistanceHeaders.containsKey("DISTACIA")) {
            distanceColumnName = normalizedDistanceHeaders.get("DISTACIA");
        } else if (normalizedDistanceHeaders.containsKey("DISTANCIA")) {
            distanceColumnName = normalizedDistanceHeaders.get("DISTANCIA");
        }
        if (distanceColumnName == null) {
            throw new IllegalArgumentException("Falta la columna de distancia en '" + DISTANCE_SHEET + "'.");
        }

        List<CoverageRecord> coverageRecords = new ArrayList<CoverageRecord>();
        Map<String, Integer> postalCodeCounter = new HashMap<String, Integer>();

        for (int rowIndex = 1; rowIndex < coverageRows.size(); rowIndex++) {
            Map<String, String> row = mapRow(coverageHeaders, coverageRows.get(rowIndex));
            String rawPostalCode = row.get(normalizedCoverageHeaders.get("CODIGO POSTAL"));
            if (rawPostalCode == null || rawPostalCode.trim().isEmpty()) {
                continue;
            }

            String postalCode;
            try {
                postalCode = normalizePostalCode(rawPostalCode);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            CoverageRecord record = new CoverageRecord(
                postalCode,
                safe(row.get(normalizedCoverageHeaders.get("SUCURSAL"))),
                safe(row.get(normalizedCoverageHeaders.get("PLAZA"))),
                safe(row.get(normalizedCoverageHeaders.get("DELEGACION/MUNICIPIO"))),
                safe(row.get(normalizedCoverageHeaders.get("CIUDAD"))),
                safe(row.get(normalizedCoverageHeaders.get("COLONIA/ASENTAMIENTO"))),
                safe(row.get(normalizedCoverageHeaders.get("ESTADO"))),
                safe(row.get(normalizedCoverageHeaders.get("COBERTURA")))
            );
            coverageRecords.add(record);
            postalCodeCounter.put(postalCode, postalCodeCounter.getOrDefault(postalCode, Integer.valueOf(0)) + 1);
        }

        Map<String, Integer> distances = new LinkedHashMap<String, Integer>();
        for (int rowIndex = 1; rowIndex < distanceRows.size(); rowIndex++) {
            Map<String, String> row = mapRow(distanceHeaders, distanceRows.get(rowIndex));
            String origin = safe(row.get(normalizedDistanceHeaders.get("ORIGEN")));
            String destination = safe(row.get(normalizedDistanceHeaders.get("DESTINO")));
            String rawDistance = safe(row.get(distanceColumnName));
            if (origin.isEmpty() || destination.isEmpty() || rawDistance.isEmpty()) {
                continue;
            }
            try {
                int distanceKm = (int) Math.round(Double.parseDouble(rawDistance));
                distances.put(distanceKey(origin, destination), Integer.valueOf(distanceKm));
            } catch (NumberFormatException ignored) {
                continue;
            }
        }

        int duplicatePostalCodes = 0;
        int duplicateRows = 0;
        for (Integer count : postalCodeCounter.values()) {
            if (count.intValue() > 1) {
                duplicatePostalCodes++;
                duplicateRows += count.intValue() - 1;
            }
        }

        return new ParsedWorkbookData(
            excelPath.toAbsolutePath(),
            coverageRecords,
            distances,
            postalCodeCounter.size(),
            coverageRecords.size(),
            duplicatePostalCodes,
            duplicateRows,
            distances.size()
        );
    }

    public static String normalizePostalCode(String rawValue) {
        return PostalCodeUtils.normalizePostalCode(rawValue);
    }

    public static String normalizeHeader(String header) {
        String normalized = Normalizer.normalize(header == null ? "" : header.trim().toUpperCase(), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    public static String distanceKey(String origin, String destination) {
        return origin + "||" + destination;
    }

    private static Map<String, String> normalizeHeaders(List<String> headers) {
        Map<String, String> normalized = new LinkedHashMap<String, String>();
        for (String header : headers) {
            normalized.put(normalizeHeader(header), header);
        }
        return normalized;
    }

    private static void requireColumns(String sheetName, Map<String, String> headers, String[] requiredColumns) {
        TreeSet<String> missing = new TreeSet<String>();
        for (String required : requiredColumns) {
            if (!headers.containsKey(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                "Faltan columnas en '" + sheetName + "': " + String.join(", ", missing)
            );
        }
    }

    private static Map<String, String> mapRow(List<String> headers, List<String> row) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (int index = 0; index < headers.size(); index++) {
            result.put(headers.get(index), index < row.size() ? row.get(index) : "");
        }
        return result;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class XlsxWorkbook {
        private final Map<String, byte[]> entries = new HashMap<String, byte[]>();
        private final List<String> sharedStrings = new ArrayList<String>();
        private final Map<String, String> sheetPaths = new LinkedHashMap<String, String>();

        private XlsxWorkbook(byte[] content) throws Exception {
            loadEntries(content);
            loadSharedStrings();
            loadSheetPaths();
        }

        private List<List<String>> readSheet(String sheetName) throws Exception {
            String path = sheetPaths.get(sheetName);
            if (path == null) {
                throw new IllegalArgumentException("No se encontro la hoja '" + sheetName + "' en el archivo.");
            }

            Document document = parseXml(entries.get(path));
            List<List<String>> rows = new ArrayList<List<String>>();
            NodeList rowNodes = document.getElementsByTagNameNS("*", "row");
            for (int rowIndex = 0; rowIndex < rowNodes.getLength(); rowIndex++) {
                Element rowElement = (Element) rowNodes.item(rowIndex);
                NodeList cellNodes = rowElement.getElementsByTagNameNS("*", "c");
                List<String> values = new ArrayList<String>();
                for (int cellIndex = 0; cellIndex < cellNodes.getLength(); cellIndex++) {
                    Element cell = (Element) cellNodes.item(cellIndex);
                    int columnIndex = columnIndexFromReference(cell.getAttribute("r"));
                    while (values.size() <= columnIndex) {
                        values.add("");
                    }
                    values.set(columnIndex, cellValue(cell));
                }
                rows.add(values);
            }
            return rows;
        }

        private void loadEntries(byte[] content) throws IOException {
            ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(content));
            try {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    entries.put(entry.getName(), zipInputStream.readAllBytes());
                }
            } finally {
                zipInputStream.close();
            }
        }

        private void loadSharedStrings() throws Exception {
            byte[] sharedStringsBytes = entries.get("xl/sharedStrings.xml");
            if (sharedStringsBytes == null) {
                return;
            }
            Document document = parseXml(sharedStringsBytes);
            NodeList items = document.getElementsByTagNameNS("*", "si");
            for (int index = 0; index < items.getLength(); index++) {
                Element item = (Element) items.item(index);
                sharedStrings.add(allText(item, "t"));
            }
        }

        private void loadSheetPaths() throws Exception {
            Document workbook = parseXml(entries.get("xl/workbook.xml"));
            Document relationships = parseXml(entries.get("xl/_rels/workbook.xml.rels"));

            Map<String, String> relationMap = new HashMap<String, String>();
            NodeList relationNodes = relationships.getElementsByTagNameNS("*", "Relationship");
            for (int index = 0; index < relationNodes.getLength(); index++) {
                Element relation = (Element) relationNodes.item(index);
                relationMap.put(relation.getAttribute("Id"), relation.getAttribute("Target"));
            }

            NodeList sheets = workbook.getElementsByTagNameNS("*", "sheet");
            for (int index = 0; index < sheets.getLength(); index++) {
                Element sheet = (Element) sheets.item(index);
                String relationId = sheet.getAttributeNS(
                    "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
                    "id"
                );
                String target = relationMap.get(relationId);
                if (target == null) {
                    continue;
                }
                if (!target.startsWith("xl/")) {
                    target = "xl/" + target;
                }
                sheetPaths.put(sheet.getAttribute("name"), target);
            }
        }

        private String cellValue(Element cell) {
            String type = cell.getAttribute("t");
            if ("inlineStr".equals(type)) {
                return allText(cell, "t");
            }
            NodeList values = cell.getElementsByTagNameNS("*", "v");
            if (values.getLength() == 0) {
                return "";
            }
            String text = values.item(0).getTextContent();
            if ("s".equals(type) && !text.isEmpty()) {
                int index = Integer.parseInt(text);
                return index >= 0 && index < sharedStrings.size() ? sharedStrings.get(index) : "";
            }
            return text == null ? "" : text;
        }

        private static Document parseXml(byte[] content) throws Exception {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(content));
        }

        private static String allText(Element element, String localName) {
            StringBuilder builder = new StringBuilder();
            NodeList textNodes = element.getElementsByTagNameNS("*", localName);
            for (int index = 0; index < textNodes.getLength(); index++) {
                Node node = textNodes.item(index);
                builder.append(node.getTextContent() == null ? "" : node.getTextContent());
            }
            return builder.toString();
        }

        private static int columnIndexFromReference(String reference) {
            int result = 0;
            for (int index = 0; index < reference.length(); index++) {
                char character = reference.charAt(index);
                if (!Character.isLetter(character)) {
                    break;
                }
                result = (result * 26) + (Character.toUpperCase(character) - 'A' + 1);
            }
            return Math.max(result - 1, 0);
        }
    }
}
