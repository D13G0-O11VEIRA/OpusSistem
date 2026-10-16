package br.opussistem.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FormatosTest {

    @Test
    void leValoresNoPadraoBrasileiroEInternacional() {
        assertEquals(new BigDecimal("1234.56"), Formatos.lerDecimal("R$ 1.234,56"));
        assertEquals(new BigDecimal("1234.56"), Formatos.lerDecimal("1234.56"));
        assertEquals(new BigDecimal("300"), Formatos.lerDecimal("300"));
        assertNull(Formatos.lerDecimal(""));
        assertNull(Formatos.lerDecimal(null));
    }

    @Test
    void leDatasEmVariosFormatos() {
        LocalDate esperado = LocalDate.of(2026, 10, 5);
        assertEquals(esperado, Formatos.lerData("05/10/2026"));
        assertEquals(esperado, Formatos.lerData("5/10/26"));
        assertEquals(esperado, Formatos.lerData("2026-10-05T10:00:00"));
        assertEquals(esperado, Formatos.lerData("20261005"));
        assertNull(Formatos.lerData("31/02/2026"));
        assertNull(Formatos.lerData("abc"));
    }

    @Test
    void formataDecimalComVirgula() {
        assertEquals("1234,50", Formatos.decimal(new BigDecimal("1234.5")));
        assertEquals("", Formatos.decimal(null));
    }
}
