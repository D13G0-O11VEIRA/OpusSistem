package br.opussistem.view.componentes;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

import java.util.Map;

/**
 * Ícones de traço (mesmo desenho da versão web). Cada ícone é um caminho SVG numa área de 24x24;
 * a cor vem do CSS (.icone) ou, opcionalmente, de uma cor informada.
 */
public final class Icones {

    private static final Map<String, String> CAMINHOS = Map.ofEntries(
            Map.entry("dash", "M4.5 3H8.5A1.5 1.5 0 0 1 10 4.5V10.5A1.5 1.5 0 0 1 8.5 12H4.5A1.5 1.5 0 0 1 3 10.5V4.5A1.5 1.5 0 0 1 4.5 3ZM15.5 3H19.5A1.5 1.5 0 0 1 21 4.5V6.5A1.5 1.5 0 0 1 19.5 8H15.5A1.5 1.5 0 0 1 14 6.5V4.5A1.5 1.5 0 0 1 15.5 3ZM15.5 12H19.5A1.5 1.5 0 0 1 21 13.5V19.5A1.5 1.5 0 0 1 19.5 21H15.5A1.5 1.5 0 0 1 14 19.5V13.5A1.5 1.5 0 0 1 15.5 12ZM4.5 16H8.5A1.5 1.5 0 0 1 10 17.5V19.5A1.5 1.5 0 0 1 8.5 21H4.5A1.5 1.5 0 0 1 3 19.5V17.5A1.5 1.5 0 0 1 4.5 16Z"),
            Map.entry("cad", "M7 4H17A2 2 0 0 1 19 6V19A2 2 0 0 1 17 21H7A2 2 0 0 1 5 19V6A2 2 0 0 1 7 4ZM9 4H15V7H9ZM12 11V17M9 14H15"),
            Map.entry("alt", "M4 20L5 16L16 5L19 8L8 19ZM14 7L17 10"),
            Map.entry("loc", "M17 11A6 6 0 1 1 5 11A6 6 0 1 1 17 11ZM20 20L15.5 15.5"),
            Map.entry("reab", "M20 11A8 8 0 1 0 17.7 16.7M20 4V11H13"),
            Map.entry("rel", "M5 20V10M12 20V4M19 20V13"),
            Map.entry("lote", "M3 21H21M14 4L20 10L11 19H5V13Z"),
            Map.entry("hist", "M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12ZM12 7V12L15 14"),
            Map.entry("alert", "M6 17V11A6 6 0 0 1 18 11V17L20 19H4ZM10 21A2 2 0 0 0 14 21"),
            Map.entry("io", "M7 4V18M3 14L7 18L11 14M17 20V6M13 10L17 6L21 10"),
            Map.entry("ok", "M5 12L10 17L19 7"),
            Map.entry("doc", "M8 3H16A2 2 0 0 1 18 5V19A2 2 0 0 1 16 21H8A2 2 0 0 1 6 19V5A2 2 0 0 1 8 3ZM9 8H15M9 12H15M9 16H12")
    );

    private Icones() {
    }

    public static Node criar(String nome, double tamanho) {
        return criar(nome, tamanho, null);
    }

    /** @param cor cor do traço (ex.: "#1d4fb0") ou null para usar a cor do tema */
    public static Node criar(String nome, double tamanho, String cor) {
        SVGPath caminho = new SVGPath();
        caminho.setContent(CAMINHOS.getOrDefault(nome, CAMINHOS.get("doc")));
        caminho.getStyleClass().add("icone");
        if (cor != null) {
            caminho.setStyle("-fx-stroke: " + cor + ";");
        }
        Rectangle area = new Rectangle(24, 24);
        area.setFill(Color.TRANSPARENT);
        Group grupo = new Group(area, caminho);
        double escala = tamanho / 24.0;
        grupo.setScaleX(escala);
        grupo.setScaleY(escala);
        StackPane caixa = new StackPane(grupo);
        caixa.setMinSize(tamanho, tamanho);
        caixa.setPrefSize(tamanho, tamanho);
        caixa.setMaxSize(tamanho, tamanho);
        caixa.setMouseTransparent(true);
        return caixa;
    }
}
