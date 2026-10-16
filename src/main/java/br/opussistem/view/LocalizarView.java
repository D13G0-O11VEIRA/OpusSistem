package br.opussistem.view;

import br.opussistem.controller.Contexto;
import br.opussistem.controller.Navegador;
import br.opussistem.controller.Rota;
import br.opussistem.model.FiltroOS;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.StatusOS;
import br.opussistem.service.ConfiguracaoService;
import br.opussistem.service.Rotulos;
import br.opussistem.util.Formatos;
import br.opussistem.view.componentes.Paginacao;
import br.opussistem.view.componentes.Tabela;
import br.opussistem.view.componentes.Ui;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/** Pesquisa de OS (filtros + resultados paginados) com atalhos para histórico, alteração e relatório. */
public class LocalizarView extends Tela {

    private static final int LIMITE = 500;
    private static final int POR_PAGINA = 8;

    private final TextField numero = Ui.texto("OS-2026-0001");
    private final TextField cliente = Ui.texto("");
    private final TextField patrimonio = Ui.texto("");
    private final TextField serie = Ui.texto("");
    private final ComboBox<String> fabricante;
    private final ComboBox<String> tecnico;
    private final ComboBox<StatusOS> status = new ComboBox<>(FXCollections.observableArrayList(StatusOS.values()));
    private final DatePicker inicio = Ui.data();
    private final DatePicker fim = Ui.data();
    private final Paginacao paginacao = new Paginacao(POR_PAGINA, this::mostrarPagina);
    private List<OrdemServico> todas = new ArrayList<>();
    private final Tabela<OrdemServico> tabela = new Tabela<OrdemServico>()
            .col("Nº OS", OrdemServico::getNumero, 110)
            .colNo(Rotulos.de("equipamento"), o -> Ui.duasLinhas(o.getEquipamento(),
                    Formatos.texto(o.getFabricante()) + " | " + Rotulos.de("patrimonio") + ": " + Formatos.texto(o.getPatrimonio())
                            + " | " + Rotulos.de("serie") + ": " + Formatos.texto(o.getNumeroSerie())), 270)
            .col(Rotulos.de("tecnico"), OrdemServico::getTecnico, 130)
            .colNo("Recebimento", o -> Ui.duasLinhas(Formatos.data(o.getDataEntrada()), Formatos.ha(o.getDataEntrada())), 110)
            .colNo("Status", o -> Ui.badgeStatus(o.getStatus()), 120)
            .colNo("Ações", o -> acoes(o), 250);

    public LocalizarView() {
        super("Localizar OS", "Pesquise por número, cliente, patrimônio, série, fabricante, técnico, status ou período.");
        ConfiguracaoService cfg = Contexto.CONFIG;
        fabricante = Ui.comboFiltro(Contexto.OS.sugestoes("fabricante", cfg.lista(ConfiguracaoService.LISTA_FABRICANTES)), "Todos");
        tecnico = Ui.comboFiltro(Contexto.OS.sugestoes("tecnico_responsavel", cfg.lista(ConfiguracaoService.LISTA_TECNICOS)), "Todos");
        status.setPromptText("Todos");

        Ui.Grade filtros = Ui.grade(4);
        filtros.adicionar(Ui.campo("Nº OS", numero)).adicionar(Ui.campo(Rotulos.de("cliente"), cliente))
                .adicionar(Ui.campo(Rotulos.de("patrimonio"), patrimonio)).adicionar(Ui.campo(Rotulos.de("serie"), serie))
                .adicionar(Ui.campo(Rotulos.de("fabricante"), fabricante)).adicionar(Ui.campo(Rotulos.de("tecnico"), tecnico))
                .adicionar(Ui.campo("Status", status))
                .adicionar(new HBox(8, Ui.campo("Entrada de", inicio), Ui.campo("até", fim)));
        for (TextField t : List.of(numero, cliente, patrimonio, serie)) {
            t.setOnAction(e -> Ui.executar(this::pesquisar));
        }
        tabela.linhasVisiveis(POR_PAGINA);

        getChildren().addAll(
                Ui.card(null, filtros, Ui.linha(Ui.botao("Pesquisar", this::pesquisar), Ui.secundario("Limpar filtros", this::limpar))),
                Ui.card("Ordens de serviço encontradas", tabela.view(), paginacao));
        pesquisar();
    }

    private HBox acoes(OrdemServico o) {
        HBox h = new HBox(6,
                Ui.pequeno(Ui.secundario("Histórico", () -> Navegador.ir(Rota.HISTORICO, o.getId()))),
                Ui.pequeno(Ui.secundario("Alterar", () -> Navegador.ir(Rota.ALTERAR, o.getId()))),
                Ui.pequeno(Ui.secundario("Relatório", () -> Navegador.ir(Rota.RELATORIOS, o.getId()))));
        return h;
    }

    private void pesquisar() {
        FiltroOS f = new FiltroOS()
                .numero(numero.getText()).cliente(cliente.getText())
                .patrimonio(patrimonio.getText()).serie(serie.getText())
                .fabricante(fabricante.getValue()).tecnico(tecnico.getValue())
                .status(status.getValue()).periodo(inicio.getValue(), fim.getValue());
        todas = new ArrayList<>(Contexto.OS.pesquisar(f, LIMITE));
        paginacao.voltarParaPrimeira();
        paginacao.definirTotal(todas.size());
        mostrarPagina();
    }

    private void mostrarPagina() {
        tabela.itens(new ArrayList<>(paginacao.fatia(todas)));
    }

    private void limpar() {
        for (TextField t : List.of(numero, cliente, patrimonio, serie)) {
            t.clear();
        }
        fabricante.setValue(null);
        tecnico.setValue(null);
        status.setValue(null);
        inicio.setValue(null);
        fim.setValue(null);
        pesquisar();
    }
}
