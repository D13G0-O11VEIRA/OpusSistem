package br.opussistem.view;

import br.opussistem.controller.Contexto;
import br.opussistem.controller.Navegador;
import br.opussistem.controller.Rota;
import br.opussistem.model.AlertaPreventiva;
import br.opussistem.model.EventoHistorico;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.ResumoMensal;
import br.opussistem.model.ResumoPainel;
import br.opussistem.service.Rotulos;
import br.opussistem.util.Formatos;
import br.opussistem.view.componentes.GraficoBarras;
import br.opussistem.view.componentes.GraficoRosca;
import br.opussistem.view.componentes.GraficoRosca.Fatia;
import br.opussistem.view.componentes.Tabela;
import br.opussistem.view.componentes.Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Visão geral: indicadores, gráficos, últimas OS, preventivas próximas, carga por técnico e atividade recente. */
public class DashboardView extends Tela {

    private static final String[] MESES = { "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez" };

    public DashboardView() {
        super("Dashboard", "Visão geral das ordens de serviço e da manutenção dos equipamentos.");
        ResumoPainel r = Contexto.DASHBOARD.resumo();
        if (r.total() == 0) {
            getChildren().add(boasVindas());
        }
        getChildren().addAll(indicadores(r), graficos(r), tabelas(), carga());
    }

    private VBox boasVindas() {
        Label texto = new Label("Ainda não há ordens de serviço. Abra a primeira pelo menu Cadastro ou traga dados de outro "
                + "sistema em Exportar / Importar (há um arquivo de exemplo em templates/modelo-importacao.csv).");
        texto.setWrapText(true);
        return Ui.card("Bem-vindo, " + Contexto.usuario().getNome() + "!", texto,
                Ui.linha(Ui.secundario("Configurar empresa e logo", () -> Navegador.ir(Rota.PERFIL))));
    }

    private FlowPane indicadores(ResumoPainel r) {
        FlowPane flow = new FlowPane(12, 12);
        flow.getChildren().addAll(
                Ui.kpi("Total de OS", String.valueOf(r.total()), "cad", "#dce8fd", "#1d4fb0"),
                Ui.kpi("OS abertas", String.valueOf(r.abertas()), "alt", "#dce8fd", "#1d4fb0"),
                Ui.kpi("OS fechadas", String.valueOf(r.fechadas()), "ok", "#d8f3e4", "#17794a"),
                Ui.kpi("Aguardando peça", String.valueOf(r.aguardandoPeca()), "hist", "#fdecd0", "#a8620a"),
                Ui.kpi("Preventivas próximas", String.valueOf(r.preventivasProximas()), "alert", "#fbdcdc", "#b42a2a"),
                Ui.kpi("Aguardando assinatura", String.valueOf(r.pendentesAssinatura()), "lote", "#ece3fb", "#6a3fc0"));
        return flow;
    }

    private Node graficos(ResumoPainel r) {
        List<String> rotulos = new ArrayList<>();
        List<Integer> abertas = new ArrayList<>();
        List<Integer> fechadas = new ArrayList<>();
        for (ResumoMensal m : Contexto.DASHBOARD.ultimosMeses(6)) {
            rotulos.add(MESES[m.mes().getMonthValue() - 1] + "/" + String.valueOf(m.mes().getYear()).substring(2));
            abertas.add(m.abertas());
            fechadas.add(m.fechadas());
        }
        HBox legenda = new HBox(14, itemLegenda("barra-azul", "Abertas"), itemLegenda("barra-verde", "Fechadas"));
        VBox barras = Ui.card("Ordens de serviço por mês", legenda, new GraficoBarras(rotulos, abertas, fechadas));

        GraficoRosca rosca = new GraficoRosca(List.of(
                new Fatia("Aberta", r.abertas()),
                new Fatia("Fechada", r.fechadas()),
                new Fatia("Aguardando Peça", r.aguardandoPeca()),
                new Fatia("Reaberta", r.reabertas())), String.valueOf(r.total()));
        VBox status = Ui.card("Status das OS", rosca);
        return Ui.colunas(new double[] { 60, 40 }, barras, status);
    }

    private HBox itemLegenda(String classe, String texto) {
        Rectangle quadrado = new Rectangle(10, 10);
        quadrado.setArcWidth(3);
        quadrado.setArcHeight(3);
        quadrado.getStyleClass().add(classe);
        Label l = new Label(texto);
        l.getStyleClass().add("dica-texto");
        HBox h = new HBox(6, quadrado, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private Node tabelas() {
        Tabela<OrdemServico> ultimas = new Tabela<OrdemServico>()
                .col("Nº OS", OrdemServico::getNumero, 100)
                .col(Rotulos.de("equipamento"), OrdemServico::getEquipamento, 130)
                .col(Rotulos.de("fabricante"), OrdemServico::getFabricante, 100)
                .col(Rotulos.de("tecnico"), OrdemServico::getTecnico, 110)
                .col("Data", o -> Formatos.data(o.getDataEntrada()), 85)
                .colNo("Status", o -> Ui.badgeStatus(o.getStatus()), 110)
                .itens(Contexto.DASHBOARD.ultimasOS(5))
                .aoDuploClique(o -> Navegador.ir(Rota.HISTORICO, o.getId()))
                .linhasVisiveis(5);

        List<AlertaPreventiva> proximas = Contexto.DASHBOARD.proximasPreventivas(5);
        Tabela<AlertaPreventiva> preventivas = new Tabela<AlertaPreventiva>()
                .col("Vencimento", a -> Formatos.data(a.vencimento()), 90)
                .col(Rotulos.de("equipamento"), a -> a.os().getEquipamento(), 130)
                .col(Rotulos.de("patrimonio"), a -> a.os().getPatrimonio(), 90)
                .col(Rotulos.de("tecnico"), a -> a.os().getTecnico(), 110)
                .colNo("Prioridade", a -> Ui.badgePrioridade(a.prioridade()), 100)
                .itens(proximas)
                .linhasVisiveis(5);

        Label dica = new Label("Dê dois cliques em uma OS para abrir o histórico.");
        dica.getStyleClass().add("dica-texto");
        return Ui.colunas(new double[] { 50, 50 },
                Ui.card("Últimas ordens de serviço", ultimas.view(), dica),
                Ui.card("Próximas preventivas", preventivas.view()));
    }

    private Node carga() {
        VBox porTecnico = new VBox(10);
        Map<String, Integer> carga = Contexto.DASHBOARD.cargaPorTecnico();
        int maximo = carga.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        if (carga.isEmpty()) {
            Label vazio = new Label("Nenhuma OS em andamento.");
            vazio.getStyleClass().add("dica-texto");
            porTecnico.getChildren().add(vazio);
        }
        for (Map.Entry<String, Integer> e : carga.entrySet()) {
            Label nome = new Label(e.getKey());
            nome.setMinWidth(140);
            nome.setMaxWidth(140);
            Region preenchimento = new Region();
            preenchimento.getStyleClass().add("barra-horizontal-preenchimento");
            preenchimento.setPrefHeight(8);
            StackPane trilho = new StackPane(preenchimento);
            trilho.getStyleClass().add("barra-horizontal-trilho");
            trilho.setPrefHeight(8);
            trilho.setMinHeight(8);
            trilho.setMaxHeight(8);
            StackPane.setAlignment(preenchimento, Pos.CENTER_LEFT);
            preenchimento.maxWidthProperty().bind(trilho.widthProperty().multiply((double) e.getValue() / maximo));
            HBox.setHgrow(trilho, Priority.ALWAYS);
            HBox linha = new HBox(10, nome, trilho, new Label(String.valueOf(e.getValue())));
            linha.setAlignment(Pos.CENTER_LEFT);
            porTecnico.getChildren().add(linha);
        }

        VBox atividade = new VBox(0);
        List<EventoHistorico> eventos = Contexto.DASHBOARD.atividadeRecente(6);
        if (eventos.isEmpty()) {
            Label vazio = new Label("Sem atividade.");
            vazio.getStyleClass().add("dica-texto");
            atividade.getChildren().add(vazio);
        }
        for (EventoHistorico ev : eventos) {
            Circle marcador = new Circle(6, Color.web(ev.getCategoria().getCor()));
            Label texto = new Label(ev.getNumeroOs() + " — " + ev.getTitulo());
            Label data = new Label(Formatos.dataHora(ev.getDataHora()));
            data.getStyleClass().add("evento-data");
            HBox linha = new HBox(12, marcador, texto, Ui.espaco(), data);
            linha.setAlignment(Pos.CENTER_LEFT);
            linha.getStyleClass().add("separador-linha");
            linha.setStyle("-fx-padding: 9 0 9 0;");
            atividade.getChildren().add(linha);
        }
        return Ui.colunas(new double[] { 50, 50 },
                Ui.card("OS em andamento por técnico", porTecnico),
                Ui.card("Atividade recente", atividade));
    }
}
