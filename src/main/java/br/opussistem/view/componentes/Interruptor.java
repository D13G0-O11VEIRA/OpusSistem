package br.opussistem.view.componentes;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/** Interruptor liga/desliga (igual aos "switches" da versão web), com texto ao lado. */
public class Interruptor extends HBox {

    private final BooleanProperty selecionado = new SimpleBooleanProperty(false);
    private final Rectangle trilho = new Rectangle(38, 22);
    private final Circle botao = new Circle(8);

    public Interruptor(String texto) {
        super(10);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("interruptor");
        trilho.setArcWidth(22);
        trilho.setArcHeight(22);
        trilho.getStyleClass().add("interruptor-trilho");
        botao.getStyleClass().add("interruptor-botao");
        StackPane pilha = new StackPane(trilho, botao);
        StackPane.setAlignment(botao, Pos.CENTER_LEFT);
        StackPane.setMargin(botao, new Insets(0, 0, 0, 3));
        pilha.setMinSize(38, 22);
        pilha.setPrefSize(38, 22);
        pilha.setMaxSize(38, 22);
        Label rotulo = new Label(texto);
        rotulo.setWrapText(true);
        getChildren().addAll(pilha, rotulo);
        setOnMouseClicked(e -> {
            if (!isDisabled()) {
                selecionado.set(!selecionado.get());
            }
        });
        selecionado.addListener((obs, antes, depois) -> atualizar());
        atualizar();
    }

    private void atualizar() {
        trilho.getStyleClass().remove("ligado");
        if (selecionado.get()) {
            trilho.getStyleClass().add("ligado");
        }
        botao.setTranslateX(selecionado.get() ? 16 : 0);
    }

    public BooleanProperty selecionadoProperty() {
        return selecionado;
    }

    public boolean isSelecionado() {
        return selecionado.get();
    }

    public void setSelecionado(boolean valor) {
        selecionado.set(valor);
    }
}
