package br.opussistem.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlTest {

    @Test
    void convertePareceHtmlEmTexto() {
        String html = "<html dir=\"ltr\"><head></head><body contenteditable=\"true\"><p>Troca do <b>sensor</b> &amp; limpeza</p>"
                + "<ul><li>item 1</li><li>item 2</li></ul></body></html>";
        String texto = Html.paraTexto(html);
        assertTrue(texto.contains("Troca do sensor & limpeza"));
        assertTrue(texto.contains("• item 1"));
        assertTrue(texto.contains("• item 2"));
    }

    @Test
    void textoSimplesPermaneceIgual() {
        assertFalse(Html.pareceHtml("apenas texto"));
        assertEquals("apenas texto", Html.paraTexto("apenas texto"));
    }

    @Test
    void detectaVazio() {
        assertTrue(Html.vazio("<html><head></head><body contenteditable=\"true\"><p></p></body></html>"));
        assertTrue(Html.vazio(null));
        assertFalse(Html.vazio("<p>x</p>"));
    }

    @Test
    void textoParaHtmlEscapaECriaParagrafos() {
        assertEquals("<p>a &lt; b</p><p>segunda</p>", Html.deTexto("a < b\nsegunda"));
    }

    @Test
    void corpoRemoveContenteditable() {
        assertEquals("<p>oi</p>", Html.corpo("<html><body contenteditable=\"true\"><p>oi</p></body></html>"));
    }
}
