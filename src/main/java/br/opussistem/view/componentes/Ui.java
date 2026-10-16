package br.opussistem.view.componentes;

import br.opussistem.exception.AppException;
import br.opussistem.exception.DaoException;
import br.opussistem.model.Prioridade;
import br.opussistem.model.StatusOS;
import br.opussistem.util.Formatos;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;

/** Fábrica de componentes visuais padronizados (estilos em css/app.css). */
public final class Ui {

    private static final System.Logger LOG = System.getLogger(Ui.class.getName());

    private Ui() {
    }

    public static String css() {
        return Ui.class.getResource("/css/app.css").toExternalForm();
    }

    // ------------------------------------------------------------ estrutura

    public static VBox cabecalho(String titulo, String subtitulo) {
        Label t = new Label(titulo);
        t.getStyleClass().add("titulo-pagina");
        Label s = new Label(subtitulo);
        s.getStyleClass().add("subtitulo");
        s.setWrapText(true);
        return new VBox(2, t, s);
    }

    public static VBox card(String titulo, Node... filhos) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        if (titulo != null) {
            Label t = new Label(titulo);
            t.getStyleClass().add("card-titulo");
            card.getChildren().add(t);
        }
        card.getChildren().addAll(filhos);
        return card;
    }

    public static VBox campo(String rotulo, Node controle) {
        Label l = new Label(rotulo);
        l.getStyleClass().add("campo-rotulo");
        if (controle instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        VBox caixa = new VBox(4, l, controle);
        caixa.getStyleClass().add("campo");
        return caixa;
    }

    public static HBox linha(Node... nos) {
        HBox h = new HBox(8, nos);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** Botões alinhados à direita. */
    public static HBox acoes(Node... nos) {
        HBox h = new HBox(8, nos);
        h.setAlignment(Pos.CENTER_RIGHT);
        return h;
    }

    public static Region espaco() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    public static ScrollPane rolagem(Node conteudo) {
        ScrollPane sp = new ScrollPane(conteudo);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("rolagem");
        return sp;
    }

    public static Window janela(Node no) {
        return no.getScene() == null ? null : no.getScene().getWindow();
    }

    /** Grade com N colunas iguais; os campos são colocados em sequência. */
    public static class Grade extends GridPane {
        private final int colunas;
        private int coluna;
        private int linha;

        public Grade(int colunas) {
            this.colunas = colunas;
            setHgap(12);
            setVgap(10);
            for (int i = 0; i < colunas; i++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(100.0 / colunas);
                getColumnConstraints().add(cc);
            }
        }

        public Grade adicionar(Node no) {
            add(no, coluna, linha);
            GridPane.setHgrow(no, Priority.ALWAYS);
            coluna++;
            if (coluna == colunas) {
                coluna = 0;
                linha++;
            }
            return this;
        }

        public Grade linhaInteira(Node no) {
            if (coluna != 0) {
                coluna = 0;
                linha++;
            }
            add(no, 0, linha, colunas, 1);
            GridPane.setHgrow(no, Priority.ALWAYS);
            linha++;
            return this;
        }
    }

    public static Grade grade(int colunas) {
        return new Grade(colunas);
    }

    /** Colunas lado a lado com larguras em percentual (ex.: {60, 40}); as colunas têm a mesma altura. */
    public static GridPane colunas(double[] percentuais, Node... nos) {
        GridPane g = new GridPane();
        g.setHgap(16);
        for (int i = 0; i < nos.length; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(percentuais[i]);
            cc.setHgrow(Priority.ALWAYS);
            g.getColumnConstraints().add(cc);
            g.add(nos[i], i, 0);
        }
        return g;
    }

    /** Texto principal com uma linha pequena e discreta embaixo (como as células da versão web). */
    public static VBox duasLinhas(String principal, String secundaria) {
        Label a = new Label(principal == null ? "" : principal);
        VBox caixa = new VBox(0, a);
        if (secundaria != null && !secundaria.isBlank()) {
            Label b = new Label(secundaria);
            b.getStyleClass().add("dica-texto");
            caixa.getChildren().add(b);
        }
        return caixa;
    }

    // ------------------------------------------------------------ botões e selos

    public static Button botao(String texto, Runnable acao) {
        Button b = new Button(texto);
        b.getStyleClass().add("primario");
        b.setOnAction(e -> executar(acao));
        return b;
    }

    public static Button secundario(String texto, Runnable acao) {
        Button b = new Button(texto);
        b.getStyleClass().add("secundario");
        b.setOnAction(e -> executar(acao));
        return b;
    }

    public static Button perigo(String texto, Runnable acao) {
        Button b = new Button(texto);
        b.getStyleClass().add("perigo");
        b.setOnAction(e -> executar(acao));
        return b;
    }

    public static Label badge(String texto, String classeCor) {
        Label l = new Label(texto);
        l.getStyleClass().addAll("badge", classeCor);
        return l;
    }

    public static Label badgeStatus(StatusOS status) {
        String cor = switch (status) {
            case ABERTA -> "badge-azul";
            case EM_ANDAMENTO -> "badge-roxo";
            case AGUARDANDO_PECA -> "badge-laranja";
            case REABERTA -> "badge-vermelho";
            case FECHADA -> "badge-verde";
        };
        return badge(status.getRotulo(), cor);
    }

    public static Label badgePrioridade(Prioridade p) {
        return badge(p.getRotulo(), p.getClasseCss());
    }

    /** Cartão de indicador com ícone colorido (igual ao da versão web). */
    public static HBox kpi(String titulo, String valor, String icone, String fundo, String cor) {
        return kpi(titulo, valor, icone, fundo, cor, null);
    }

    public static HBox kpi(String titulo, String valor, String icone, String fundo, String cor, String subtitulo) {
        StackPane caixaIcone = new StackPane(Icones.criar(icone, 20, cor));
        caixaIcone.setMinSize(40, 40);
        caixaIcone.setPrefSize(40, 40);
        caixaIcone.setMaxSize(40, 40);
        caixaIcone.getStyleClass().add("kpi-icone");
        caixaIcone.setStyle("-fx-background-color: " + fundo + ";");

        Label t = new Label(titulo);
        t.getStyleClass().add("kpi-titulo");
        Label v = new Label(valor);
        v.getStyleClass().add("kpi-valor");
        v.setStyle("-fx-text-fill: " + cor + ";");
        VBox textos = new VBox(1, t, v);
        if (subtitulo != null) {
            Label s = new Label(subtitulo);
            s.getStyleClass().add("kpi-titulo");
            s.setStyle("-fx-text-fill: " + cor + ";");
            textos.getChildren().add(s);
        }
        HBox card = new HBox(12, caixaIcone, textos);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14, 18, 14, 16));
        card.setMinWidth(200);
        return card;
    }

    /** Quadro pequeno "título + valor" (resumo do pacote, por exemplo). */
    public static VBox tile(String titulo, String valor) {
        Label t = new Label(titulo);
        t.getStyleClass().add("tile-titulo");
        Label v = new Label(valor);
        v.getStyleClass().add("destaque-texto");
        v.setWrapText(true);
        VBox caixa = new VBox(2, t, v);
        caixa.getStyleClass().add("tile");
        return caixa;
    }

    /** Faixa de aviso (vermelha) ou de "tudo certo" (verde), no topo da tela de alertas. */
    public static HBox faixa(String texto, boolean ok) {
        Label l = new Label(texto);
        l.setWrapText(true);
        HBox caixa = new HBox(l);
        caixa.getStyleClass().add("faixa-aviso");
        if (ok) {
            caixa.getStyleClass().add("faixa-ok");
        }
        return caixa;
    }

    /** Caixa de observação azul clara. */
    public static VBox nota(String texto) {
        Label l = new Label(texto);
        l.setWrapText(true);
        VBox caixa = new VBox(l);
        caixa.getStyleClass().add("nota");
        return caixa;
    }

    public static Button pequeno(Button b) {
        b.getStyleClass().add("pequeno");
        return b;
    }

    /** Botão de OS/seleção em linha (chips): preenchido quando ativo, contornado quando não. */
    public static Button chip(String texto, boolean ativo, Runnable acao) {
        Button b = ativo ? botao(texto, acao) : secundario(texto, acao);
        return pequeno(b);
    }

    /** Círculo com a inicial do nome (usuário na barra superior). */
    public static StackPane avatar(String nome) {
        String letra = nome == null || nome.isBlank() ? "?" : nome.trim().substring(0, 1).toUpperCase();
        Label l = new Label(letra);
        StackPane c = new StackPane(l);
        c.getStyleClass().add("avatar");
        c.setMinSize(24, 24);
        c.setPrefSize(24, 24);
        c.setMaxSize(24, 24);
        return c;
    }

    // ------------------------------------------------------------ campos

    public static TextField texto(String prompt) {
        TextField t = new TextField();
        t.setPromptText(prompt);
        return t;
    }

    public static DatePicker data() {
        DatePicker dp = new DatePicker();
        dp.setPromptText("dd/mm/aaaa");
        dp.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : Formatos.DATA.format(d);
            }

            @Override
            public LocalDate fromString(String s) {
                return Formatos.lerData(s);
            }
        });
        return dp;
    }

    /** Lista com digitação livre (as opções são sugestões). */
    public static ComboBox<String> comboEditavel(List<String> itens) {
        ComboBox<String> c = new ComboBox<>(FXCollections.observableArrayList(itens));
        c.setEditable(true);
        return c;
    }

    /** Lista fechada com opção de "nenhum" (valor nulo = todos). */
    public static ComboBox<String> comboFiltro(List<String> itens, String prompt) {
        ComboBox<String> c = new ComboBox<>(FXCollections.observableArrayList(itens));
        c.setPromptText(prompt);
        return c;
    }

    /** Texto digitado/selecionado ou null se vazio. */
    public static String valor(ComboBox<String> c) {
        String t = c.isEditable() ? c.getEditor().getText() : c.getValue();
        return t == null || t.isBlank() ? null : t.trim();
    }

    public static void definir(ComboBox<String> c, String valor) {
        c.setValue(valor);
        if (c.isEditable()) {
            c.getEditor().setText(valor == null ? "" : valor);
        }
    }

    public static String valor(TextField t) {
        return t.getText() == null || t.getText().isBlank() ? null : t.getText().trim();
    }

    // ------------------------------------------------------------ erros

    /** Executa uma ação de tela mostrando mensagens amigáveis em caso de erro. */
    public static void executar(Runnable acao) {
        try {
            acao.run();
        } catch (AppException e) {
            Dialogos.erro(e.getMessage());
        } catch (DaoException e) {
            LOG.log(System.Logger.Level.ERROR, "Erro de banco de dados", e);
            Dialogos.erro("Erro ao acessar o banco de dados.\n" + e.getMessage());
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR, "Erro inesperado", e);
            Dialogos.erro("Ocorreu um erro inesperado: " + e);
        }
    }
}
