package br.opussistem.view.componentes;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.List;

/** Gráfico de barras duplas (abertas x fechadas por mês) com o valor sobre cada barra, como na versão web. */
public class GraficoBarras extends HBox {

    private static final double ALTURA_MAXIMA = 150;
    private static final double LARGURA_BARRA = 24;

    public GraficoBarras(List<String> rotulos, List<Integer> abertas, List<Integer> fechadas) {
        super(14);
        setAlignment(Pos.BOTTOM_CENTER);
        setMinHeight(ALTURA_MAXIMA + 50);
        int maximo = 1;
        for (int i = 0; i < rotulos.size(); i++) {
            maximo = Math.max(maximo, Math.max(abertas.get(i), fechadas.get(i)));
        }
        for (int i = 0; i < rotulos.size(); i++) {
            HBox barras = new HBox(4, barra(abertas.get(i), maximo, "barra-azul"), barra(fechadas.get(i), maximo, "barra-verde"));
            barras.setAlignment(Pos.BOTTOM_CENTER);
            Label rotulo = new Label(rotulos.get(i));
            rotulo.getStyleClass().add("grafico-rotulo");
            VBox coluna = new VBox(6, barras, rotulo);
            coluna.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(coluna, Priority.ALWAYS);
            coluna.setMaxWidth(Double.MAX_VALUE);
            getChildren().add(coluna);
        }
    }

    private static VBox barra(int valor, int maximo, String classe) {
        Label numero = new Label(String.valueOf(valor));
        numero.getStyleClass().add("grafico-valor");
        Rectangle retangulo = new Rectangle(LARGURA_BARRA, Math.max(2, ALTURA_MAXIMA * valor / maximo));
        retangulo.setArcWidth(6);
        retangulo.setArcHeight(6);
        retangulo.getStyleClass().add(classe);
        VBox caixa = new VBox(2, numero, retangulo);
        caixa.setAlignment(Pos.BOTTOM_CENTER);
        return caixa;
    }
}
