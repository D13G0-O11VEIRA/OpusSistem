package br.opussistem.view.componentes;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;

import java.util.Optional;

/** Caixas de diálogo padronizadas. */
public final class Dialogos {

    private Dialogos() {
    }

    public static void erro(String mensagem) {
        mostrar(Alert.AlertType.ERROR, mensagem);
    }

    public static void info(String mensagem) {
        mostrar(Alert.AlertType.INFORMATION, mensagem);
    }

    public static boolean confirmar(String mensagem) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensagem, ButtonType.YES, ButtonType.NO);
        a.setTitle("OpusSistem");
        a.setHeaderText(null);
        a.getDialogPane().getStylesheets().add(Ui.css());
        IconeApp.aplicar(a.getDialogPane().getScene().getWindow());
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private static void mostrar(Alert.AlertType tipo, String mensagem) {
        Alert a = new Alert(tipo, mensagem, ButtonType.OK);
        a.setTitle("OpusSistem");
        a.setHeaderText(null);
        a.getDialogPane().getStylesheets().add(Ui.css());
        IconeApp.aplicar(a.getDialogPane().getScene().getWindow());
        a.showAndWait();
    }

    /** Pede um texto de várias linhas (por exemplo, o motivo de uma reabertura). */
    public static Optional<String> pedirTextoLongo(String titulo, String cabecalho) {
        Dialog<String> d = new Dialog<>();
        d.setTitle(titulo);
        d.setHeaderText(cabecalho);
        TextArea area = new TextArea();
        area.setPrefRowCount(5);
        area.setWrapText(true);
        d.getDialogPane().setContent(area);
        d.getDialogPane().getStylesheets().add(Ui.css());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.setResultConverter(botao -> botao == ButtonType.OK ? area.getText() : null);
        IconeApp.aplicar(d.getDialogPane().getScene().getWindow());
        Platform.runLater(area::requestFocus);
        return d.showAndWait();
    }
}
