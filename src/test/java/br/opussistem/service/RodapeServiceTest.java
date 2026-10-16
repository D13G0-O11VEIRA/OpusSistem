package br.opussistem.service;

import br.opussistem.exception.AppException;
import br.opussistem.service.RodapeService.Efetivo;
import br.opussistem.service.RodapeService.Origem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RodapeServiceTest {

    @Test
    void rodapeDaOsTemPrioridadeSobreOsDemais() {
        Efetivo e = RodapeService.escolher("fale com a gente", "cliente@exemplo.com", "geral");
        assertEquals(Origem.OS, e.origem());
        assertEquals("fale com a gente", e.texto());
    }

    @Test
    void semRodapeDaOsUsaOdoCliente() {
        Efetivo e = RodapeService.escolher("  ", "enviar relatório para compras@cliente.com", "geral");
        assertEquals(Origem.CLIENTE, e.origem());
        assertEquals("enviar relatório para compras@cliente.com", e.texto());
    }

    @Test
    void semRodapeDeOsNemClienteUsaOGeral() {
        Efetivo e = RodapeService.escolher(null, null, "Documento emitido pelo OpusSistem");
        assertEquals(Origem.GERAL, e.origem());
    }

    @Test
    void semNenhumRodapeDevolveVazio() {
        Efetivo e = RodapeService.escolher(null, "", " ");
        assertEquals(Origem.NENHUM, e.origem());
        assertTrue(e.vazio());
    }

    @Test
    void normalizaEspacosEFinsDeLinha() {
        assertEquals("linha 1\nlinha 2", RodapeService.normalizar("  linha 1   \r\nlinha 2  \r\n\r\n"));
    }

    @Test
    void validaOLimiteDeCaracteres() {
        assertEquals("ok", RodapeService.validar(" ok "));
        String longo = "x".repeat(RodapeService.LIMITE_CARACTERES + 1);
        assertThrows(AppException.class, () -> RodapeService.validar(longo));
    }
}
