package br.opussistem.view.componentes;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Construtor fluente de TableView (evita PropertyValueFactory e reflexão). */
public class Tabela<S> {

    private final TableView<S> view = new TableView<>();

    public Tabela() {
        view.setPlaceholder(new Label("Nenhum registro encontrado."));
        view.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /** Ajusta a altura da tabela para mostrar n linhas (cabeçalho de 38 px + linhas de 42 px). */
    public Tabela<S> linhasVisiveis(int n) {
        double altura = 40 + 42.0 * n;
        view.setFixedCellSize(42);
        view.setPrefHeight(altura);
        view.setMinHeight(altura);
        view.setMaxHeight(altura);
        return this;
    }

    /** Coluna de texto/valor simples. */
    public Tabela<S> col(String titulo, Function<S, ?> valor, double largura) {
        TableColumn<S, Object> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<Object>(valor.apply(cd.getValue())));
        c.setPrefWidth(largura);
        view.getColumns().add(c);
        return this;
    }

    /** Coluna com um componente visual (por exemplo, um selo colorido). */
    public Tabela<S> colNo(String titulo, Function<S, ? extends Node> no, double largura) {
        TableColumn<S, Object> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<Object>(cd.getValue()));
        c.setCellFactory(coluna -> new TableCell<S, Object>() {
            @Override
            @SuppressWarnings("unchecked")
            protected void updateItem(Object item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(null);
                setGraphic(vazio || item == null ? null : no.apply((S) item));
            }
        });
        c.setPrefWidth(largura);
        view.getColumns().add(c);
        return this;
    }

    public Tabela<S> itens(List<S> lista) {
        view.setItems(FXCollections.observableArrayList(lista));
        return this;
    }

    public Tabela<S> aoDuploClique(Consumer<S> acao) {
        view.setRowFactory(tv -> {
            TableRow<S> linha = new TableRow<>();
            linha.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !linha.isEmpty()) {
                    acao.accept(linha.getItem());
                }
            });
            return linha;
        });
        return this;
    }

    public Tabela<S> aoSelecionar(Consumer<S> acao) {
        view.getSelectionModel().selectedItemProperty().addListener((obs, antes, depois) -> acao.accept(depois));
        return this;
    }

    public S selecionado() {
        return view.getSelectionModel().getSelectedItem();
    }

    public TableView<S> view() {
        return view;
    }
}
