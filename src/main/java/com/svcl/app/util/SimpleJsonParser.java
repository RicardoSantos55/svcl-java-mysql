package com.svcl.app.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SimpleJsonParser {
    private final String text;
    private int index;

    private SimpleJsonParser(String text) {
        this.text = text;
    }

    public static Map<String, String> parseObject(String input) {
        return new SimpleJsonParser(input).parseObject();
    }

    private Map<String, String> parseObject() {
        skipWhitespace();
        expect('{');
        skipWhitespace();

        Map<String, String> result = new LinkedHashMap<String, String>();
        if (peek() == '}') {
            index++;
            return result;
        }

        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            String value = parseValueAsString();
            result.put(key, value);
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                index++;
                continue;
            }
            if (next == '}') {
                index++;
                break;
            }
            throw new IllegalArgumentException("JSON invalido.");
        }

        skipWhitespace();
        if (index != text.length()) {
            throw new IllegalArgumentException("JSON invalido.");
        }
        return result;
    }

    private String parseValueAsString() {
        char current = peek();
        if (current == '"') {
            return parseString();
        }
        int start = index;
        while (index < text.length()) {
            char token = text.charAt(index);
            if (token == ',' || token == '}') {
                break;
            }
            index++;
        }
        return text.substring(start, index).trim();
    }

    private String parseString() {
        expect('"');
        StringBuilder builder = new StringBuilder();
        while (index < text.length()) {
            char current = text.charAt(index++);
            if (current == '"') {
                return builder.toString();
            }
            if (current == '\\') {
                if (index >= text.length()) {
                    throw new IllegalArgumentException("Escape invalido en JSON.");
                }
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                        builder.append(escaped);
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'u':
                        if (index + 4 > text.length()) {
                            throw new IllegalArgumentException("Unicode invalido en JSON.");
                        }
                        String hex = text.substring(index, index + 4);
                        builder.append((char) Integer.parseInt(hex, 16));
                        index += 4;
                        break;
                    default:
                        throw new IllegalArgumentException("Escape invalido en JSON.");
                }
            } else {
                builder.append(current);
            }
        }
        throw new IllegalArgumentException("Cadena JSON sin cerrar.");
    }

    private void skipWhitespace() {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
    }

    private void expect(char expected) {
        if (peek() != expected) {
            throw new IllegalArgumentException("JSON invalido.");
        }
        index++;
    }

    private char peek() {
        if (index >= text.length()) {
            return '\0';
        }
        return text.charAt(index);
    }
}
