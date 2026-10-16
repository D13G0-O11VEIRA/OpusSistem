package br.opussistem.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelatorioHtmlTest {

    @Test
    void textoSimplesEhEscapadoEPreservaQuebras() {
        assertEquals("a &lt; b<br>segunda &amp; terceira", RelatorioHtml.rico("a < b\nsegunda & terceira"));
    }

    @Test
    void vazioViraEspacoNaoQuebravel() {
        assertEquals("&nbsp;", RelatorioHtml.rico(null));
        assertEquals("&nbsp;", RelatorioHtml.rico("  "));
    }
}
