package br.opussistem.view.componentes;

import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

/**
 * Prévia da folha do relatório (HTML) dentro do programa, com o mesmo desenho do PDF.
 * JavaScript fica desligado: o conteúdo é apenas exibido.
 */
public class PreviaRelatorio extends StackPane {

    private final WebView web = new WebView();

    /**
     * @param largura largura do componente em pixels
     * @param altura  altura do componente em pixels
     * @param zoom    escala do conteúdo (a folha tem 595 px de largura; 0,8 cabe em ~520 px)
     */
    public PreviaRelatorio(double largura, double altura, double zoom) {
        getStyleClass().add("papel");
        web.getEngine().setJavaScriptEnabled(false);
        web.setContextMenuEnabled(false);
        web.setZoom(zoom);
        web.setPrefSize(largura, altura);
        web.setMinSize(largura, altura);
        getChildren().add(web);
    }

    public void mostrar(String html) {
        web.getEngine().loadContent(html, "text/html");
    }
}
