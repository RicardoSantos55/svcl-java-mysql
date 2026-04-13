package com.svcl.app.util;

public final class PostalCodeUtils {
    private PostalCodeUtils() {
    }

    public static String normalizePostalCode(String rawValue) {
        StringBuilder digits = new StringBuilder();
        for (char character : rawValue.toCharArray()) {
            if (Character.isDigit(character)) {
                digits.append(character);
            }
        }
        if (digits.length() == 0 || digits.length() > 5) {
            throw new IllegalArgumentException("El codigo postal debe contener entre 1 y 5 digitos.");
        }
        while (digits.length() < 5) {
            digits.insert(0, '0');
        }
        return digits.toString();
    }
}
