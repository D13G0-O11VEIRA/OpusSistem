package br.opussistem.view;

import br.opussistem.App;
import br.opussistem.controller.Contexto;
import br.opussistem.model.Assinatura;
import br.opussistem.model.FiltroOS;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.PapelAssinatura;
import br.opussistem.model.StatusOS;
import br.opussistem.service.ConfiguracaoService;
import br.opussistem.service.Rotulos;
import br.opussistem.service.RodapeService;
import br.opussistem.util.Formatos;
import br.opussistem.util.Html;
import br.opussistem.view.componentes.DialogoAssinatura;
import br.opussistem.view.componentes.Dialogos;
import br.opussistem.view.componentes.PreviaRelatorio;
import br.opussistem.view.componentes.Tabela;
import br.opussistem.view.componentes.Toast;
import br.opussistem.view.componentes.Ui;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Relatório de Atendimento Técnico: prévia da folha, assinaturas digitais (cliente e técnico), PDF e o
 * texto de rodapé (por OS ou para todos os relatórios de um cliente). A observação da OS não é impressa.
 */
public class RelatoriosView extends Tela {

    private static final String SO_ESTA_OS = "Somente esta OS";
    private static final String TODO_CLIENTE = "Todos os relatórios deste cliente";

    private final TextField numero = Ui.texto("OS-2026-0001");
    private final TextField cliente = Ui.texto("");
    private final DatePicker inicio = Ui.data();
    private final DatePicker fim = Ui.data();
    private final ComboBox<StatusOS> status = new ComboBox<>(FXCollections.observableArrayList(StatusOS.values()));
    private final Tabela<OrdemServico> tabela = new Tabela<OrdemServico>()
            .col("Nº OS", OrdemServico::getNumero, 105)
            .colNo(Rotulos.de("equipamento"), o -> Ui.duasLinhas(o.getEquipamento(),
                    Formatos.texto(o.getFabricante()) + " | " + Rotulos.de("serie") + ": " + Formatos.texto(o.getNumeroSerie())), 170)
            .col("Data", o -> Formatos.data(o.dataReferencia()), 85)
            .colNo("Status", o -> Ui.badgeStatus(o.getStatus()), 105);
    private final PreviaRelatorio previa = new PreviaRelatorio(540, 730, 0.82);
    private final HBox barraAssinaturas = new HBox(8);
    private final VBox cartaoRodape = new VBox();
    private final VBox colunaDireita = new VBox(16);
    private OrdemServico atual;

    public RelatoriosView(Long idInicial) {
        super("Relatório de OS", "Consulte, gere e imprima o Relatório de Atendimento Técnico, colete assinaturas e defina o rodapé da folha.");
        status.setPromptText("Todos");
        for (TextField t : List.of(numero, cliente)) {
            t.setOnAction(e -> Ui.executar(this::pesquisar));
        }
        Ui.Grade filtros = Ui.grade(5);
        filtros.adicionar(Ui.campo("Nº OS", numero)).adicionar(Ui.campo(Rotulos.de("cliente"), cliente))
                .adicionar(Ui.campo("Período de", inicio)).adicionar(Ui.campo("até", fim)).adicionar(Ui.campo("Status", status));
        tabela.linhasVisiveis(10);
        tabela.aoSelecionar(os -> Ui.executar(() -> abrir(os)));

        VBox esquerda = Ui.card("Ordens de serviço encontradas", tabela.view());
        barraAssinaturas.setAlignment(Pos.CENTER_LEFT);
        VBox folha = Ui.card("Relatório de Atendimento Técnico", barraAssinaturas, previa);
        colunaDireita.getChildren().addAll(folha, cartaoRodape);
        colunaDireita.setVisible(false);
        colunaDireita.setManaged(false);

        getChildren().addAll(Ui.card(null, filtros, Ui.linha(Ui.botao("Pesquisar", this::pesquisar),
                Ui.secundario("Limpar filtros", this::limpar))), Ui.colunas(new double[] { 38, 62 }, esquerda, colunaDireita));
        pesquisar();
        if (idInicial != null) {
            tabela.view().getItems().stream().filter(o -> o.getId().equals(idInicial)).findFirst().ifPresentOrElse(
                    o -> tabela.view().getSelectionModel().select(o),
                    () -> Contexto.OS.buscar(idInicial).ifPresent(this::abrir));
        }
    }

    // ------------------------------------------------------------ lista

    private void pesquisar() {
        FiltroOS f = new FiltroOS().numero(numero.getText()).cliente(cliente.getText())
                .status(status.getValue()).periodo(inicio.getValue(), fim.getValue());
        tabela.itens(Contexto.OS.pesquisar(f, 80));
    }

    private void limpar() {
        numero.clear();
        cliente.clear();
        inicio.setValue(null);
        fim.setValue(null);
        status.setValue(null);
        pesquisar();
    }

    // ------------------------------------------------------------ painel da OS escolhida

    private void abrir(OrdemServico escolhida) {
        if (escolhida == null) {
            atual = null;
            colunaDireita.setVisible(false);
            colunaDireita.setManaged(false);
            return;
        }
        atual = Contexto.OS.buscar(escolhida.getId()).orElse(escolhida);
        colunaDireita.setVisible(true);
        colunaDireita.setManaged(true);
        atualizarBarra();
        previa.mostrar(Contexto.PREVIA.paginaDaOS(atual));
        montarRodape();
    }

    private void atualizarBarra() {
        OrdemServico os = atual;
        Map<PapelAssinatura, Assinatura> ass = Contexto.ASSINATURAS.daOS(os.getId());
        ConfiguracaoService cfg = Contexto.CONFIG;
        barraAssinaturas.getChildren().clear();
        barraAssinaturas.getChildren().add(controleAssinatura(os, PapelAssinatura.CLIENTE,
                cfg.get(ConfiguracaoService.REL_ASSINATURA1), ass.get(PapelAssinatura.CLIENTE)));
        barraAssinaturas.getChildren().add(controleAssinatura(os, PapelAssinatura.TECNICO,
                cfg.get(ConfiguracaoService.REL_ASSINATURA2), ass.get(PapelAssinatura.TECNICO)));
        barraAssinaturas.getChildren().addAll(Ui.espaco(), Ui.pequeno(Ui.botao("Imprimir / salvar PDF", this::gerarPdf)));
    }

    private HBox controleAssinatura(OrdemServico os, PapelAssinatura papel, String rotulo, Assinatura a) {
        HBox caixa = new HBox(4);
        caixa.setAlignment(Pos.CENTER_LEFT);
        if (a != null) {
            boolean valida = Contexto.ASSINATURAS.valida(a, os);
            Label chip = Ui.badge("✓ " + rotulo + ": " + a.getNome() + (valida ? "" : " (documento alterado)"),
                    valida ? "badge-verde" : "badge-vermelho");
            caixa.getChildren().addAll(chip, Ui.pequeno(Ui.secundario("×", () -> remover(os, papel))));
        } else {
            Button assinar = Ui.pequeno(Ui.secundario("Assinar como " + rotulo.toLowerCase(), () -> assinar(os, papel)));
            if (os.getStatus() != StatusOS.FECHADA) {
                assinar.setTooltip(new Tooltip("A OS ainda está aberta: o programa pergunta se deve fechá-la antes de assinar."));
            }
            caixa.getChildren().add(assinar);
        }
        return caixa;
    }

    private void assinar(OrdemServico escolhida, PapelAssinatura papel) {
        OrdemServico os = Contexto.OS.buscar(escolhida.getId()).orElse(escolhida);
        if (os.getStatus() != StatusOS.FECHADA) {
            String aviso = "A OS " + os.getNumero() + " ainda não está fechada. A assinatura vale para o relatório final, "
                    + "por isso a OS precisa ser fechada antes."
                    + (Html.vazio(os.getLaudoTecnico()) ? "\n\nAtenção: o laudo técnico desta OS está vazio." : "")
                    + "\n\nFechar a OS agora e continuar com a assinatura?";
            if (!Dialogos.confirmar(aviso)) {
                return;
            }
            os = Contexto.OS.fechar(os.getId(), Contexto.usuario());
            recarregar(os.getId()); // atualiza o status na lista e o painel da direita
        }
        boolean porCliente = papel == PapelAssinatura.CLIENTE;
        byte[] salva = porCliente ? null : Contexto.AUTH.lerAssinatura(Contexto.usuario()).orElse(null);
        Optional<DialogoAssinatura.Resultado> r = DialogoAssinatura.mostrar(
                "OS " + os.getNumero() + " — assinatura do " + (porCliente ? "cliente" : "técnico"),
                porCliente ? Formatos.texto(os.getCliente()) : Contexto.usuario().getNome(), porCliente, salva);
        if (r.isPresent()) {
            DialogoAssinatura.Resultado d = r.get();
            Contexto.ASSINATURAS.assinar(os.getId(), papel, d.nome(), d.email(), d.png(), Contexto.usuario());
            Toast.mostrar("Assinatura registrada");
            recarregar(os.getId());
        }
    }

    /** Refaz a lista e seleciona de novo a OS (mantém o painel da direita em dia). */
    private void recarregar(long id) {
        pesquisar();
        tabela.view().getItems().stream().filter(o -> o.getId() == id).findFirst().ifPresentOrElse(
                o -> tabela.view().getSelectionModel().select(o),
                () -> Contexto.OS.buscar(id).ifPresent(this::abrir));
    }

    private void remover(OrdemServico os, PapelAssinatura papel) {
        Contexto.ASSINATURAS.remover(os.getId(), papel, Contexto.usuario());
        Toast.mostrar("Assinatura removida");
        abrir(os);
    }

    private void gerarPdf() {
        Path pdf = Contexto.RELATORIOS.gerar(Contexto.OS.buscar(atual.getId()).orElse(atual));
        Toast.mostrar("PDF gerado: " + pdf);
        App.abrir(pdf);
    }

    // ------------------------------------------------------------ rodapé da folha

    private void montarRodape() {
        OrdemServico os = atual;
        RodapeService.Efetivo efetivo = Contexto.RODAPES.efetivo(os);
        boolean temCliente = os.getCliente() != null && !os.getCliente().isBlank();

        TextArea texto = new TextArea(efetivo.origem() == RodapeService.Origem.GERAL ? "" : efetivo.texto());
        texto.setPrefRowCount(3);
        texto.setWrapText(true);
        texto.setPromptText("Ex.: Dúvidas sobre este relatório: contato@empresa-do-cliente.com");
        Label contador = new Label();
        contador.getStyleClass().add("dica-texto");
        Runnable atualizarContador = () -> contador.setText(texto.getText().length() + " / " + RodapeService.LIMITE_CARACTERES);
        texto.textProperty().addListener((obs, antes, depois) -> atualizarContador.run());
        atualizarContador.run();

        ComboBox<String> escopo = new ComboBox<>(FXCollections.observableArrayList(SO_ESTA_OS, TODO_CLIENTE));
        escopo.setValue(temCliente && efetivo.origem() != RodapeService.Origem.OS ? TODO_CLIENTE : SO_ESTA_OS);
        escopo.setDisable(!temCliente);

        String emUso = efetivo.origem() == RodapeService.Origem.NENHUM ? "nenhum rodapé definido"
                : "rodapé " + efetivo.origem().getRotulo();
        Label info = new Label("Em uso neste relatório: " + emUso + ".");
        info.setWrapText(true);
        info.getStyleClass().add("dica-texto");
        Label regra = new Label("Prioridade: rodapé da OS > rodapé do cliente > rodapé geral (Perfil > Empresa e relatório). "
                + "O texto aparece centralizado no pé da folha final, abaixo das assinaturas.");
        regra.setWrapText(true);
        regra.getStyleClass().add("dica-texto");

        cartaoRodape.getChildren().setAll(Ui.card("Texto no rodapé da folha",
                info, Ui.campo("Texto do rodapé (ex.: e-mail que o cliente pede para constar)", texto), contador,
                Ui.campo("Aplicar a", escopo), regra,
                Ui.acoes(Ui.secundario("Remover rodapé específico", () -> {
                    texto.clear();
                    salvarRodape(os, "", escopo.getValue());
                }), Ui.botao("Salvar rodapé", () -> salvarRodape(os, texto.getText(), escopo.getValue())))));
    }

    private void salvarRodape(OrdemServico os, String texto, String escopo) {
        if (TODO_CLIENTE.equals(escopo)) {
            Contexto.RODAPES.salvarDoCliente(os.getCliente(), texto);
            if (os.getRodapeRelatorio() != null && !os.getRodapeRelatorio().isBlank()) {
                Contexto.OS.salvarRodape(os.getId(), null, Contexto.usuario());
            }
            Toast.mostrar(texto == null || texto.isBlank() ? "Rodapé do cliente removido" : "Rodapé salvo para todos os relatórios do cliente");
        } else {
            Contexto.OS.salvarRodape(os.getId(), texto, Contexto.usuario());
            Toast.mostrar(texto == null || texto.isBlank() ? "Rodapé desta OS removido" : "Rodapé salvo nesta OS");
        }
        abrir(os);
    }
}
