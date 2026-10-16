package br.opussistem.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigradorBancoTest {

    @Test
    void dividePorPontoEVirgulaIgnorandoComentarios() {
        String script = "-- comentário\nCREATE TABLE a (id INT);\n\n-- outro\nALTER TABLE a\n    ADD COLUMN x INT;\n";
        List<String> comandos = MigradorBanco.dividir(script);
        assertEquals(2, comandos.size());
        assertTrue(comandos.get(0).startsWith("CREATE TABLE a"));
        assertTrue(comandos.get(1).contains("ADD COLUMN x INT"));
    }
}
