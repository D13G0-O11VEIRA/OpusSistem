package br.opussistem.view.componentes;

import br.opussistem.util.Html;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;

/**
 * Editor de texto formatado (negrito, itálico, sublinhado, listas, cores, alinhamento...).
 * Guarda o resultado como HTML do corpo; o PDF respeita essa formatação.
 */
public class EditorTexto extends VBox {

    private final HTMLEditor editor = new HTMLEditor();

    public EditorTexto(double altura) {
        editor.setPrefHeight(altura);
        editor.setMinHeight(altura);
        getChildren().add(editor);
    }

    public void definir(String valor) {
        editor.setHtmlText(Html.deTexto(valor == null ? "" : valor));
    }

    /** HTML do corpo ou null se estiver vazio. */
    public String valor() {
        String html = editor.getHtmlText();
        return Html.vazio(html) ? null : Html.corpo(html);
    }
}
