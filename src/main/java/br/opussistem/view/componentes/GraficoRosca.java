package br.opussistem.view.componentes;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

import java.util.List;
import java.util.Locale;

/** Gráfico de rosca com o total no centro e legenda ao lado (igual ao "Status das OS" da versão web). */
public class GraficoRosca extends HBox {

    private static final double TAMANHO = 150;
    private static final double LARGURA_ANEL = 27;

    public record Fatia(String rotulo, int valor) {
    }

    /** As cores seguem a ordem das fatias (classes CSS fatia-0..3). */
    public GraficoRosca(List<Fatia> fatias, String textoTotal) {
        super(20);
        setAlignment(Pos.CENTER_LEFT);
        int total = fatias.stream().mapToInt(Fatia::valor).sum();

        Pane desenho = new Pane();
        desenho.setMinSize(TAMANHO, TAMANHO);
        desenho.setPrefSize(TAMANHO, TAMANHO);
        desenho.setMaxSize(TAMANHO, TAMANHO);
        double raio = (TAMANHO - LARGURA_ANEL) / 2;
        double centro = TAMANHO / 2;
        if (total == 0) {
            desenho.getChildren().add(arco(centro, raio, 90, -359.9, "fatia-vazia"));
        } else {
            double inicio = 90;
            for (int i = 0; i < fatias.size(); i++) {
                int valor = fatias.get(i).valor();
                if (valor > 0) {
                    double angulo = 360.0 * valor / total;
                    desenho.getChildren().add(arco(centro, raio, inicio, -Math.min(angulo, 359.9), "fatia-" + Math.min(i, 3)));
                    inicio -= angulo;
                }
            }
        }
        Label centroTexto = new Label(textoTotal);
        centroTexto.getStyleClass().add("rosca-total");
        Label rotuloTotal = new Label("Total");
        VBox miolo = new VBox(0, centroTexto, rotuloTotal);
        miolo.setAlignment(Pos.CENTER);
        StackPane grafico = new StackPane(desenho, miolo);

        VBox legenda = new VBox(10);
        legenda.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < fatias.size(); i++) {
            Fatia f = fatias.get(i);
            Rectangle quadrado = new Rectangle(10, 10);
            quadrado.setArcWidth(3);
            quadrado.setArcHeight(3);
            quadrado.getStyleClass().add("legenda-" + Math.min(i, 3));
            double pct = total == 0 ? 0 : 100.0 * f.valor() / total;
            Label texto = new Label(f.rotulo() + "  " + f.valor() + " (" + String.format(Locale.of("pt", "BR"), "%.1f", pct) + "%)");
            HBox linha = new HBox(8, quadrado, texto);
            linha.setAlignment(Pos.CENTER_LEFT);
            legenda.getChildren().add(linha);
        }
        getChildren().addAll(grafico, legenda);
    }

    private static Arc arco(double centro, double raio, double inicio, double comprimento, String classe) {
        Arc arco = new Arc(centro, centro, raio, raio, inicio, comprimento);
        arco.setType(ArcType.OPEN);
        arco.setFill(Color.TRANSPARENT);
        arco.setStrokeWidth(LARGURA_ANEL);
        arco.setStrokeLineCap(StrokeLineCap.BUTT);
        arco.getStyleClass().addAll("fatia", classe);
        return arco;
    }
}
