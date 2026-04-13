package com.svcl.app;

import com.svcl.app.util.PostalCodeUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoberturaTest {
    @Test
    void normalizaCodigoPostalA5Digitos() {
        assertEquals("01234", PostalCodeUtils.normalizePostalCode("1234"));
    }

    @Test
    void rechazaCodigoPostalConMasDeCincoDigitos() {
        assertThrows(IllegalArgumentException.class, () -> PostalCodeUtils.normalizePostalCode("123456"));
    }
}
