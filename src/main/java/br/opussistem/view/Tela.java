package br.opussistem.view;

import br.opussistem.view.componentes.Ui;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

/** Base das telas: margem, espaçamento e cabeçalho com título e subtítulo. */
public abstract class Tela extends VBox {

    protected Tela(String titulo, String subtitulo) {
        super(16);
        setPadding(new Insets(20, 24, 40, 24));
        getStyleClass().add("tela");
        getChildren().add(Ui.cabecalho(titulo, subtitulo));
    }
}
