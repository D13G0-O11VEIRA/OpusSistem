package br.opussistem.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuebraTest {

    /** Cada caractere "mede" 1 unidade. */
    private static double medir(String s) {
        return s.length();
    }

    @Test
    void quebraPalavrasQueNaoCabem() {
        List<String> linhas = Quebra.linhas("contato com o suporte tecnico", 14, QuebraTest::medir, 10);
        assertEquals(List.of("contato com o", "suporte", "tecnico"), linhas);
    }

    @Test
    void preservaQuebrasDeLinhaDoUsuario() {
        List<String> linhas = Quebra.linhas("Dúvidas?\nfale com suporte@exemplo.com", 60, QuebraTest::medir, 10);
        assertEquals(List.of("Dúvidas?", "fale com suporte@exemplo.com"), linhas);
    }

    @Test
    void cortaPalavraMaiorQueALinha() {
        List<String> linhas = Quebra.linhas("abcdefghij", 4, QuebraTest::medir, 10);
        assertEquals(List.of("abcd", "efgh", "ij"), linhas);
    }

    @Test
    void limitaQuantidadeDeLinhasComReticencias() {
        List<String> linhas = Quebra.linhas("a\nb\nc\nd", 10, QuebraTest::medir, 2);
        assertEquals(2, linhas.size());
        assertEquals("b…", linhas.get(1));
    }

    @Test
    void textoVazioNaoGeraLinhas() {
        assertTrue(Quebra.linhas("  \n ", 10, QuebraTest::medir, 5).isEmpty());
        assertTrue(Quebra.linhas(null, 10, QuebraTest::medir, 5).isEmpty());
    }

    @Test
    void ignoraLinhasEmBrancoNoFim() {
        assertEquals(List.of("oi"), Quebra.linhas("oi\n\n\n", 10, QuebraTest::medir, 5));
    }
}
