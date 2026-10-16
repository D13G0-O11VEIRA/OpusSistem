package br.opussistem.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenhasTest {

    @Test
    void confereSenhaCorretaERejeitaIncorreta() {
        String hash = Senhas.gerarHash("segredo123");
        assertTrue(hash.startsWith("pbkdf2$"));
        assertTrue(Senhas.conferir("segredo123", hash));
        assertFalse(Senhas.conferir("segredo124", hash));
        assertFalse(Senhas.conferir("segredo123", "lixo"));
    }

    @Test
    void cadaHashUsaSaltDiferente() {
        assertNotEquals(Senhas.gerarHash("igual"), Senhas.gerarHash("igual"));
    }
}
