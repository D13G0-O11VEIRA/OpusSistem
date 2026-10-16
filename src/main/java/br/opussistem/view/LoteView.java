package br.opussistem.view;

import br.opussistem.App;
import br.opussistem.controller.Contexto;
import br.opussistem.exception.AppException;
import br.opussistem.model.FiltroOS;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.PacoteAssinatura;
import br.opussistem.service.Rotulos;
import br.opussistem.util.Formatos;
import br.opussistem.view.componentes.Dialogos;
import br.opussistem.view.componentes.Interruptor;
import br.opussistem.view.componentes.PainelAssinatura;
import br.opussistem.view.componentes.PreviaRelatorio;
import br.opussistem.view.componentes.Tabela;
import br.opussistem.view.componentes.Toast;
import br.opussistem.view.componentes.Ui;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/**
 * Assinatura em lote: o cliente revisa vários relatórios fechados e assina uma única vez; a mesma assinatura
 * passa a constar no relatório de cada OS do pacote (igual à tela da versão web).
 */
public class LoteView extends Tela {

    private static final String PRONTO = "Pronto para assinatura";
    private static final String ASSINADO = "Assinado";
    private static final String TODOS = "Todos";

    private static final class Linha {
        final OrdemServico os;
        final boolean assinada;
        final BooleanProperty marcada = new SimpleBooleanProperty(false);

        Linha(OrdemServico os) {
            this.os = os;
            this.assinada = os.getAssinaturaCliente() != null;
        }
    }

    private final ComboBox<String> cliente = new ComboBox<>();
    private final ComboBox<String> situacao = new ComboBox<>(FXCollections.observableArrayList(PRONTO, ASSINADO, TODOS));
    private final DatePicker inicio = Ui.data();
    private final DatePicker fim = Ui.data();
    private final ObservableList<Linha> linhas = FXCollections.observableArrayList();
    private final TableView<Linha> tabela = new TableView<>(linhas);
    private final CheckBox marcarTodas = new CheckBox("Selecionar todas");
    private final HBox passos = new HBox(22);
    private final Label contagem = new Label();
    private final FlowPane resumo = new FlowPane(8, 8);
    private final HBox chipsPaginas = new HBox(6);
    private final PreviaRelatorio previa = new PreviaRelatorio(380, 540, 0.58);
    private final Interruptor revisei = new Interruptor("Confirmo que revisei todas as OS selecionadas");
    private final VBox painelAssinatura = new VBox(10);
    private final TextField nome = Ui.texto("Nome do responsável");
    private final TextField email = Ui.texto("opcional");
    private final PainelAssinatura painel = new PainelAssinatura();
    private final CheckBox concordo = new CheckBox("Li e estou de acordo com os relatórios deste pacote");
    private final Tabela<PacoteAssinatura> pacotes = new Tabela<PacoteAssinatura>()
            .col("Pacote", PacoteAssinatura::codigo, 150)
            .col(Rotulos.de("cliente"), PacoteAssinatura::cliente, 180)
            .col("OS", PacoteAssinatura::quantidade, 50)
            .col("Assinado por", PacoteAssinatura::assinante, 160)
            .col("Data", p -> Formatos.dataHora(p.assinadoEm()), 130)
            .colNo("Ações", p -> Ui.pequeno(Ui.secundario("Abrir PDF", () -> abrirPacote(p.codigo(), p.cliente()))), 110);
    private String paginaAtual = "capa";

    public LoteView() {
        super("Assinatura em lote", "Reúna várias ordens de serviço do mesmo cliente em um único pacote para revisão e assinatura digital.");
        cliente.setItems(FXCollections.observableArrayList(Contexto.OS.clientesComOSFechadas()));
        cliente.setPromptText("Selecione o cliente");
        situacao.setValue(PRONTO);
        if (!cliente.getItems().isEmpty()) {
            cliente.setValue(cliente.getItems().get(0));
        }

        Ui.Grade filtros = Ui.grade(5);
        filtros.adicionar(Ui.campo(Rotulos.de("cliente"), cliente)).adicionar(Ui.campo("Período de", inicio))
                .adicionar(Ui.campo("até", fim)).adicionar(Ui.campo("Status do relatório", situacao))
                .adicionar(Ui.campo(" ", Ui.botao("Buscar OS", this::buscar)));

        passos.setAlignment(Pos.CENTER_LEFT);
        contagem.getStyleClass().add("destaque-texto");
        HBox cabecalhoPassos = new HBox(22, passos, Ui.espaco(), contagem, Ui.secundario("Limpar seleção", this::limparSelecao));
        cabecalhoPassos.setAlignment(Pos.CENTER_LEFT);

        marcarTodas.setOnAction(e -> {
            for (Linha l : linhas) {
                l.marcada.set(marcarTodas.isSelected());
            }
        });
        revisei.selecionadoProperty().addListener((obs, antes, depois) -> atualizarEstado());
        concordo.setWrapText(true);

        HBox barraInferior = new HBox(16,
                new Label("O cliente assina uma única vez e confirma o recebimento e a ciência dos relatórios selecionados."),
                Ui.espaco(), revisei, Ui.secundario("Imprimir pacote (PDF)", this::visualizar));
        barraInferior.setAlignment(Pos.CENTER_LEFT);
        barraInferior.getStyleClass().add("faixa-confirmacao");

        getChildren().addAll(
                Ui.card(null, filtros),
                Ui.card(null, cabecalhoPassos),
                Ui.colunas(new double[] { 38, 32, 30 }, cartaoOS(), cartaoPacote(), cartaoAssinatura()),
                barraInferior,
                Ui.card("Pacotes assinados", pacotes.linhasVisiveis(4).view()));

        cliente.valueProperty().addListener((obs, antes, depois) -> buscar());
        situacao.valueProperty().addListener((obs, antes, depois) -> buscar());
        buscar();
        recarregarPacotes();
    }

    // ------------------------------------------------------------ montagem

    private VBox cartaoOS() {
        tabela.setEditable(true);
        TableColumn<Linha, Boolean> marcar = new TableColumn<>("");
        marcar.setCellValueFactory(cd -> cd.getValue().marcada);
        marcar.setCellFactory(CheckBoxTableCell.forTableColumn(marcar));
        marcar.setEditable(true);
        marcar.setPrefWidth(40);
        tabela.getColumns().add(marcar);
        texto("Nº OS", l -> l.os.getNumero(), 105);
        texto(Rotulos.de("equipamento"), l -> Formatos.texto(l.os.getEquipamento()), 130);
        texto(Rotulos.de("patrimonio"), l -> Formatos.texto(l.os.getPatrimonio()), 80);
        texto("Data", l -> Formatos.data(l.os.getDataEntrega()), 85);
        texto("Status", l -> l.assinada ? "Assinado" : "Pronto", 80);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setFixedCellSize(42);
        tabela.setPrefHeight(40 + 42 * 6);
        tabela.setMinHeight(40 + 42 * 6);
        tabela.setPlaceholder(new Label("Nenhuma OS fechada para este cliente e filtro."));
        VBox card = Ui.card("Ordens de serviço do cliente", marcarTodas, tabela,
                Ui.nota("Cada OS continua armazenada individualmente no histórico, mas fica vinculada ao mesmo pacote e à mesma assinatura digital."));
        return card;
    }

    private void texto(String titulo, Function<Linha, String> valor, double largura) {
        TableColumn<Linha, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(valor.apply(cd.getValue())));
        c.setPrefWidth(largura);
        tabela.getColumns().add(c);
    }

    private VBox cartaoPacote() {
        return Ui.card("Pacote de atendimento", resumo, chipsPaginas, previa);
    }

    private VBox cartaoAssinatura() {
        Label titulo = new Label("Próximo passo: assinatura do cliente");
        titulo.getStyleClass().add("card-titulo");
        Label dica = new Label("Revise o pacote e ative \"Confirmo que revisei\" (barra inferior) para liberar a assinatura.");
        dica.setWrapText(true);
        dica.getStyleClass().add("dica-texto");
        Button assinar = (Button) Ui.botao("Assinar pacote", this::assinar);
        assinar.setMaxWidth(Double.MAX_VALUE);
        painelAssinatura.getChildren().addAll(dica, Ui.campo("Nome do responsável", nome), Ui.campo("E-mail", email),
                Ui.campo("Assinatura digital", painel), Ui.linha(Ui.pequeno(Ui.secundario("Limpar assinatura", painel::limpar))),
                concordo, assinar);
        return Ui.card(null, titulo, painelAssinatura);
    }

    // ------------------------------------------------------------ dados

    private void buscar() {
        linhas.clear();
        marcarTodas.setSelected(false);
        String escolhido = cliente.getValue();
        if (escolhido != null) {
            FiltroOS filtro = new FiltroOS().somenteFechadas(true).cliente(escolhido).periodo(inicio.getValue(), fim.getValue());
            for (OrdemServico os : Contexto.OS.pesquisar(filtro, 1000)) {
                if (!escolhido.equalsIgnoreCase(os.getCliente())) {
                    continue;
                }
                boolean assinada = os.getAssinaturaCliente() != null;
                String filtroSituacao = situacao.getValue();
                if (PRONTO.equals(filtroSituacao) && assinada || ASSINADO.equals(filtroSituacao) && !assinada) {
                    continue;
                }
                Linha l = new Linha(os);
                l.marcada.addListener((obs, antes, depois) -> atualizarEstado());
                linhas.add(l);
            }
        }
        paginaAtual = "capa";
        atualizarEstado();
    }

    private void limparSelecao() {
        for (Linha l : linhas) {
            l.marcada.set(false);
        }
        revisei.setSelecionado(false);
        marcarTodas.setSelected(false);
    }

    private List<OrdemServico> selecionadas() {
        return linhas.stream().filter(l -> l.marcada.get()).map(l -> l.os).toList();
    }

    private void atualizarEstado() {
        List<OrdemServico> sel = selecionadas();
        int n = sel.size();
        int etapa = n == 0 ? 1 : (!revisei.isSelecionado() ? 2 : 3);
        passos.getChildren().setAll(passo(1, "Selecionar OS", etapa), passo(2, "Revisar pacote", etapa),
                passo(3, "Assinar digitalmente", etapa));
        contagem.setText(n + (n == 1 ? " OS selecionada" : " OS selecionadas"));

        boolean liberada = n > 0 && revisei.isSelecionado();
        painelAssinatura.setDisable(!liberada);
        painelAssinatura.getStyleClass().remove("bloqueado");
        if (!liberada) {
            painelAssinatura.getStyleClass().add("bloqueado");
        }

        resumo.getChildren().clear();
        long equipamentos = sel.stream().map(o -> Formatos.texto(o.getPatrimonio()) + "|" + Formatos.texto(o.getNumeroSerie())).distinct().count();
        String periodo = n == 0 ? "—" : Formatos.data(sel.stream().map(OrdemServico::getDataEntrada).min(java.util.Comparator.naturalOrder()).orElse(null))
                + " a " + Formatos.data(sel.stream().map(OrdemServico::dataReferencia).max(java.util.Comparator.naturalOrder()).orElse(null));
        resumo.getChildren().addAll(Ui.tile(Rotulos.de("cliente"), cliente.getValue() == null ? "—" : cliente.getValue()),
                Ui.tile("Relatórios", String.valueOf(n)), Ui.tile(Rotulos.de("equipamento") + "s", String.valueOf(equipamentos)),
                Ui.tile("Período", periodo));
        boolean todosAssinados = n > 0 && sel.stream().allMatch(o -> o.getAssinaturaCliente() != null);
        resumo.getChildren().add(Ui.tile("Status do pacote", n == 0 ? "Nenhuma OS selecionada"
                : todosAssinados ? "Assinado" : PRONTO));

        chipsPaginas.getChildren().clear();
        if (n > 0) {
            if (!"capa".equals(paginaAtual) && sel.stream().noneMatch(o -> String.valueOf(o.getId()).equals(paginaAtual))) {
                paginaAtual = "capa";
            }
            chipsPaginas.getChildren().add(Ui.chip("Capa", "capa".equals(paginaAtual), () -> trocarPagina("capa")));
            for (OrdemServico o : sel.stream().limit(6).toList()) {
                String id = String.valueOf(o.getId());
                chipsPaginas.getChildren().add(Ui.chip(o.getNumero().replaceFirst("^OS-", ""), id.equals(paginaAtual), () -> trocarPagina(id)));
            }
            mostrarPrevia(sel);
        } else {
            previa.mostrar(Contexto.PREVIA.documento("<div style=\"text-align:center;color:#6b7690;margin-top:60px\">"
                    + "Marque as OS do pacote para ver a prévia.</div>"));
        }
    }

    private HBox passo(int numero, String texto, int etapaAtual) {
        Label bola = new Label(numero < etapaAtual ? "✓" : String.valueOf(numero));
        bola.getStyleClass().add("passo-num");
        if (numero < etapaAtual) {
            bola.getStyleClass().add("feito");
        } else if (numero == etapaAtual) {
            bola.getStyleClass().add("atual");
        }
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("passo-texto");
        if (numero <= etapaAtual) {
            rotulo.getStyleClass().add("ativo");
        }
        HBox h = new HBox(8, bola, rotulo);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private void trocarPagina(String pagina) {
        paginaAtual = pagina;
        atualizarEstado();
    }

    private void mostrarPrevia(List<OrdemServico> sel) {
        if ("capa".equals(paginaAtual)) {
            previa.mostrar(Contexto.PREVIA.capa(cliente.getValue(), sel));
        } else {
            sel.stream().filter(o -> String.valueOf(o.getId()).equals(paginaAtual)).findFirst()
                    .ifPresent(o -> previa.mostrar(Contexto.PREVIA.paginaDaOS(Contexto.OS.buscar(o.getId()).orElse(o))));
        }
    }

    // ------------------------------------------------------------ ações

    private void visualizar() {
        List<OrdemServico> sel = exigirSelecao();
        Path pdf = Contexto.RELATORIOS.gerarPacote(cliente.getValue(), sel, "previa");
        App.abrir(pdf);
    }

    private void assinar() {
        List<OrdemServico> sel = exigirSelecao();
        if (!revisei.isSelecionado()) {
            throw new AppException("Confirme que revisou todas as OS selecionadas.");
        }
        if (Ui.valor(nome) == null) {
            throw new AppException("Informe o nome do responsável.");
        }
        if (painel.vazio()) {
            throw new AppException("Desenhe a assinatura no quadro.");
        }
        if (!concordo.isSelected()) {
            throw new AppException("Marque a concordância com os relatórios do pacote.");
        }
        PacoteAssinatura pacote = Contexto.ASSINATURAS.assinarPacote(sel.stream().map(OrdemServico::getId).toList(),
                nome.getText(), email.getText(), painel.png(), Contexto.usuario());
        Toast.mostrar("Pacote " + pacote.codigo() + " assinado (" + sel.size() + " OS)");
        painel.limpar();
        revisei.setSelecionado(false);
        concordo.setSelected(false);
        buscar();
        recarregarPacotes();
        if (Dialogos.confirmar("Pacote assinado. Deseja abrir o PDF do pacote agora?")) {
            abrirPacote(pacote.codigo(), pacote.cliente());
        }
    }

    private List<OrdemServico> exigirSelecao() {
        List<OrdemServico> sel = selecionadas();
        if (cliente.getValue() == null || sel.isEmpty()) {
            throw new AppException("Selecione o cliente e marque ao menos uma OS.");
        }
        return sel;
    }

    private void recarregarPacotes() {
        pacotes.itens(Contexto.ASSINATURAS.pacotes(30));
    }

    private void abrirPacote(String codigo, String nomeCliente) {
        List<OrdemServico> lista = Contexto.ASSINATURAS.osDoPacote(codigo).stream()
                .map(id -> Contexto.OS.buscar(id).orElse(null))
                .filter(java.util.Objects::nonNull).toList();
        if (lista.isEmpty()) {
            throw new AppException("As OS deste pacote não estão mais disponíveis.");
        }
        App.abrir(Contexto.RELATORIOS.gerarPacote(nomeCliente, lista, codigo));
    }
}
