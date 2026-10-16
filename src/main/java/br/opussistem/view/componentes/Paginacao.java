package br.opussistem.view.componentes;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.util.List;

/** Paginação "Mostrando 1 a 8 de 24 registros" com botões numerados (igual à versão web). */
public class Paginacao extends HBox {

    private final int tamanhoPagina;
    private final Runnable aoMudar;
    private final Label informacao = new Label();
    private final HBox botoes = new HBox(4);
    private int pagina = 1;
    private int total;

    public Paginacao(int tamanhoPagina, Runnable aoMudar) {
        super(8);
        this.tamanhoPagina = tamanhoPagina;
        this.aoMudar = aoMudar;
        setAlignment(Pos.CENTER_LEFT);
        informacao.getStyleClass().add("dica-texto");
        getChildren().addAll(informacao, Ui.espaco(), botoes);
    }

    public void voltarParaPrimeira() {
        pagina = 1;
    }

    public void definirTotal(int novoTotal) {
        total = novoTotal;
        pagina = Math.min(pagina, paginas());
        redesenhar();
    }

    /** Trecho da lista que pertence à página atual. */
    public <T> List<T> fatia(List<T> todos) {
        int inicio = Math.min((pagina - 1) * tamanhoPagina, todos.size());
        int fim = Math.min(inicio + tamanhoPagina, todos.size());
        return todos.subList(inicio, fim);
    }

    private int paginas() {
        return Math.max(1, (int) Math.ceil(total / (double) tamanhoPagina));
    }

    private void redesenhar() {
        int primeiro = total == 0 ? 0 : (pagina - 1) * tamanhoPagina + 1;
        int ultimo = Math.min(total, pagina * tamanhoPagina);
        informacao.setText("Mostrando " + primeiro + " a " + ultimo + " de " + total + " registros");
        botoes.getChildren().clear();
        for (int i = 1; i <= paginas(); i++) {
            int destino = i;
            Button b = new Button(String.valueOf(i));
            b.getStyleClass().addAll("pequeno", i == pagina ? "primario" : "secundario");
            b.setOnAction(e -> {
                pagina = destino;
                redesenhar();
                aoMudar.run();
            });
            botoes.getChildren().add(b);
        }
    }
}
