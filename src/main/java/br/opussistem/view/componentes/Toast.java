package br.opussistem.view.componentes;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Mensagem rápida que aparece na parte inferior da janela. */
public final class Toast {

    private static StackPane raiz;

    private Toast() {
    }

    public static void registrar(StackPane painelRaiz) {
        raiz = painelRaiz;
    }

    public static void mostrar(String mensagem) {
        if (raiz == null) {
            return;
        }
        Label rotulo = new Label(mensagem);
        rotulo.getStyleClass().add("toast");
        rotulo.setMouseTransparent(true);
        StackPane.setAlignment(rotulo, Pos.BOTTOM_CENTER);
        StackPane.setMargin(rotulo, new Insets(0, 0, 28, 0));
        raiz.getChildren().add(rotulo);
        PauseTransition pausa = new PauseTransition(Duration.seconds(2.8));
        pausa.setOnFinished(e -> raiz.getChildren().remove(rotulo));
        pausa.play();
    }
}
