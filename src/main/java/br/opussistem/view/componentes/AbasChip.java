package br.opussistem.view.componentes;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** Abas em forma de botões pequenos (como as abas de Perfil na versão web). O conteúdo é criado ao abrir a aba. */
public class AbasChip extends VBox {

    private final HBox barra = new HBox(6);
    private final StackPane conteudo = new StackPane();
    private final List<Button> botoes = new ArrayList<>();
    private final List<Supplier<Node>> fabricas = new ArrayList<>();
    private IntConsumer aoMudar;

    public AbasChip() {
        super(14);
        getChildren().addAll(barra, conteudo);
    }

    public AbasChip adicionar(String titulo, Supplier<Node> fabrica) {
        int indice = botoes.size();
        Button botao = new Button(titulo);
        botao.getStyleClass().add("pequeno");
        botao.setOnAction(e -> Ui.executar(() -> selecionar(indice)));
        botoes.add(botao);
        fabricas.add(fabrica);
        barra.getChildren().add(botao);
        return this;
    }

    public int quantidade() {
        return fabricas.size();
    }

    /** Chamado com o índice da aba sempre que ela muda. */
    public AbasChip aoMudar(IntConsumer acao) {
        this.aoMudar = acao;
        return this;
    }

    public void selecionar(int indice) {
        if (indice < 0 || indice >= fabricas.size()) {
            return;
        }
        if (aoMudar != null) {
            aoMudar.accept(indice);
        }
        for (int i = 0; i < botoes.size(); i++) {
            Button b = botoes.get(i);
            b.getStyleClass().removeAll("primario", "secundario");
            b.getStyleClass().add(i == indice ? "primario" : "secundario");
        }
        conteudo.getChildren().setAll(fabricas.get(indice).get());
    }
}
