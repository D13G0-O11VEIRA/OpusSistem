package br.opussistem.view;

import br.opussistem.controller.Contexto;
import br.opussistem.controller.Navegador;
import br.opussistem.controller.Rota;
import br.opussistem.model.Assinatura;
import br.opussistem.model.EventoHistorico;
import br.opussistem.model.FiltroOS;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.PapelAssinatura;
import br.opussistem.model.StatusOS;
import br.opussistem.service.ConfiguracaoService;
import br.opussistem.service.Rotulos;
import br.opussistem.util.Formatos;
import br.opussistem.util.Html;
import br.opussistem.view.componentes.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Linha do tempo completa de uma OS e seus dados atuais. */
public class HistoricoView extends Tela {

    private final TextField busca = Ui.texto("Número da OS, cliente ou patrimônio");
    private final FlowPane chips = new FlowPane(6, 6);
    private final VBox detalhe = new VBox(16);
    private List<OrdemServico> encontradas = List.of();
    private OrdemServico selecionada;

    public HistoricoView(Long idInicial) {
        super("Histórico de OS", "Consulte todas as alterações e movimentações da ordem de serviço.");
        busca.setOnAction(e -> Ui.executar(this::pesquisar));
        javafx.scene.layout.HBox.setHgrow(busca, javafx.scene.layout.Priority.ALWAYS);
        getChildren().addAll(Ui.card(null, Ui.linha(busca, Ui.botao("Pesquisar", this::pesquisar)), chips), detalhe);

        if (idInicial != null) {
            Contexto.OS.buscar(idInicial).ifPresent(os -> {
                encontradas = List.of(os);
                selecionar(os);
            });
        } else {
            encontradas = Contexto.OS.pesquisar(new FiltroOS(), 12);
            if (!encontradas.isEmpty()) {
                selecionar(encontradas.get(0));
            }
        }
        redesenharChips();
    }

    private void pesquisar() {
        String t = busca.getText() == null ? "" : busca.getText().trim();
        LinkedHashSet<OrdemServico> achadas = new LinkedHashSet<>();
        if (t.isEmpty()) {
            achadas.addAll(Contexto.OS.pesquisar(new FiltroOS(), 12));
        } else {
            achadas.addAll(Contexto.OS.pesquisar(new FiltroOS().numero(t), 12));
            achadas.addAll(Contexto.OS.pesquisar(new FiltroOS().cliente(t), 12));
            achadas.addAll(Contexto.OS.pesquisar(new FiltroOS().patrimonio(t), 12));
        }
        encontradas = List.copyOf(achadas);
        if (!encontradas.isEmpty()) {
            selecionar(encontradas.get(0));
        } else {
            selecionada = null;
            detalhe.getChildren().clear();
        }
        redesenharChips();
    }

    private void redesenharChips() {
        chips.getChildren().clear();
        for (OrdemServico os : encontradas) {
            boolean ativo = selecionada != null && selecionada.getId().equals(os.getId());
            chips.getChildren().add(Ui.chip(os.getNumero(), ativo, () -> {
                selecionar(os);
                redesenharChips();
            }));
        }
    }

    private void selecionar(OrdemServico escolhida) {
        selecionada = escolhida;
        detalhe.getChildren().clear();
        OrdemServico os = Contexto.OS.buscar(escolhida.getId()).orElse(escolhida);
        List<EventoHistorico> eventos = Contexto.OS.historico(os.getId());
        String ultima = eventos.isEmpty() ? "" : Formatos.dataHora(eventos.get(eventos.size() - 1).getDataHora());

        FlowPane resumo = new FlowPane(12, 12);
        resumo.getChildren().addAll(
                resumoItem("OS", os.getNumero()),
                resumoItem(Rotulos.de("equipamento"), Formatos.texto(os.getEquipamento())),
                resumoItem(Rotulos.de("fabricante"), Formatos.texto(os.getFabricante())),
                resumoItem("Status", os.getStatus().getRotulo()),
                resumoItem("Última atualização", ultima));
        detalhe.getChildren().addAll(resumo,
                Ui.colunas(new double[] { 58, 42 }, cartaoLinhaDoTempo(eventos), cartaoDados(os)));
    }

    private VBox resumoItem(String titulo, String valor) {
        Label v = new Label(valor.isBlank() ? "—" : valor);
        v.getStyleClass().add("card-titulo");
        Label t = new Label(titulo);
        t.getStyleClass().add("kpi-titulo");
        VBox caixa = new VBox(2, t, v);
        caixa.getStyleClass().add("card");
        caixa.setPadding(new Insets(10, 16, 10, 16));
        caixa.setMinWidth(170);
        return caixa;
    }

    private VBox cartaoLinhaDoTempo(List<EventoHistorico> eventos) {
        VBox linha = new VBox(0);
        for (EventoHistorico e : eventos) {
            Circle marcador = new Circle(6, Color.web(e.getCategoria().getCor()));
            VBox marcadorCaixa = new VBox(marcador);
            marcadorCaixa.setPadding(new Insets(5, 0, 0, 0));

            Label data = new Label(Formatos.dataHora(e.getDataHora()));
            data.getStyleClass().add("evento-data");
            Label titulo = new Label(e.getTitulo());
            titulo.setStyle("-fx-font-weight: bold; -fx-text-fill: " + e.getCategoria().getCor() + ";");
            VBox texto = new VBox(2, data, titulo);
            if (e.getDescricao() != null && !e.getDescricao().isBlank()) {
                Label desc = new Label(e.getDescricao());
                desc.setWrapText(true);
                desc.setMaxWidth(380);
                texto.getChildren().add(desc);
            }
            Label quem = new Label("Responsável:");
            quem.getStyleClass().add("evento-data");
            Label nome = new Label(Formatos.texto(e.getResponsavel()));
            nome.getStyleClass().add("evento-titulo");
            VBox direita = new VBox(0, quem, nome);
            direita.setAlignment(Pos.TOP_RIGHT);

            HBox item = new HBox(12, marcadorCaixa, texto, Ui.espaco(), direita);
            item.setAlignment(Pos.TOP_LEFT);
            item.getStyleClass().add("separador-linha");
            item.setPadding(new Insets(9, 0, 9, 0));
            linha.getChildren().add(item);
        }
        return Ui.card("Linha do tempo da OS", linha);
    }

    private VBox cartaoDados(OrdemServico os) {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(6);
        int[] linha = { 0 };
        adicionar(g, linha, Rotulos.de("cliente"), os.getCliente());
        adicionar(g, linha, "Telefone", os.getTelefone());
        adicionar(g, linha, Rotulos.de("equipamento"), os.getEquipamento());
        adicionar(g, linha, Rotulos.de("modelo"), os.getModelo());
        adicionar(g, linha, Rotulos.de("fabricante"), os.getFabricante());
        adicionar(g, linha, Rotulos.de("patrimonio"), os.getPatrimonio());
        adicionar(g, linha, Rotulos.de("serie"), os.getNumeroSerie());
        adicionar(g, linha, Rotulos.de("statusEquipamento"), os.getStatusEquipamento());
        adicionar(g, linha, Rotulos.de("manutencao"), os.getTipoManutencao());
        adicionar(g, linha, Rotulos.de("tecnico"), os.getTecnico());
        adicionar(g, linha, "Entrada", Formatos.data(os.getDataEntrada()));
        adicionar(g, linha, "Fechamento", Formatos.data(os.getDataEntrega()));
        adicionar(g, linha, "Próxima preventiva", Formatos.data(os.getProximaPreventiva()));
        adicionar(g, linha, "Valor final", Formatos.moeda(os.getValorFinal()));
        adicionar(g, linha, Rotulos.de("natureza"), Html.paraTexto(os.getDescricaoProblema()));
        adicionar(g, linha, Rotulos.de("laudo"), Html.paraTexto(os.getLaudoTecnico()));
        adicionar(g, linha, Rotulos.de("observacoes"), Html.paraTexto(os.getObservacoes()));
        adicionar(g, linha, "Rodapé do relatório", Contexto.RODAPES.efetivo(os).texto());
        adicionar(g, linha, "Assinaturas", assinaturas(os));

        Button alterar = Ui.secundario("Alterar OS", () -> Navegador.ir(Rota.ALTERAR, os.getId()));
        alterar.setDisable(os.getStatus() == StatusOS.FECHADA);
        Button reabrir = Ui.secundario("Reabrir OS", () -> Navegador.ir(Rota.REABRIR, os.getId()));
        reabrir.setDisable(os.getStatus() != StatusOS.FECHADA);
        Button relatorio = Ui.botao("Relatório e assinaturas", () -> Navegador.ir(Rota.RELATORIOS, os.getId()));
        FlowPane botoes = new FlowPane(8, 8, alterar, reabrir, relatorio);
        return Ui.card("Dados atuais da OS", Ui.badgeStatus(os.getStatus()), (Node) g, botoes);
    }

    private String assinaturas(OrdemServico os) {
        Map<PapelAssinatura, Assinatura> mapa = Contexto.ASSINATURAS.daOS(os.getId());
        ConfiguracaoService cfg = Contexto.CONFIG;
        return descreverAssinatura(cfg.get(ConfiguracaoService.REL_ASSINATURA1), mapa.get(PapelAssinatura.CLIENTE)) + "\n"
                + descreverAssinatura(cfg.get(ConfiguracaoService.REL_ASSINATURA2), mapa.get(PapelAssinatura.TECNICO));
    }

    private static String descreverAssinatura(String papel, Assinatura a) {
        return papel + ": " + (a == null ? "pendente" : "✓ " + a.getNome() + " em " + Formatos.dataHora(a.getAssinadoEm()));
    }

    private static void adicionar(GridPane g, int[] linha, String rotulo, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        Label r = new Label(rotulo + ":");
        r.getStyleClass().add("destaque-texto");
        Label v = new Label(valor);
        v.setWrapText(true);
        v.setMaxWidth(260);
        g.add(r, 0, linha[0]);
        g.add(v, 1, linha[0]);
        linha[0]++;
    }
}
