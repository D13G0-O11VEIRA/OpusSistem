package br.opussistem.view;

import br.opussistem.controller.Contexto;
import br.opussistem.controller.Navegador;
import br.opussistem.controller.Rota;
import br.opussistem.model.AlertaPreventiva;
import br.opussistem.model.Prioridade;
import br.opussistem.service.ConfiguracaoService;
import br.opussistem.service.Rotulos;
import br.opussistem.util.Csv;
import br.opussistem.util.Formatos;
import br.opussistem.view.componentes.Icones;
import br.opussistem.view.componentes.Interruptor;
import br.opussistem.view.componentes.Tabela;
import br.opussistem.view.componentes.Toast;
import br.opussistem.view.componentes.Ui;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Alertas de preventivas e calibrações: avisos 3, 2 e 1 mês antes do vencimento, programação direta de uma
 * OS preventiva e regras ajustáveis (igual à tela da versão web).
 */
public class AlertasView extends Tela {

    private static final String ATIVOS = "Alertas ativos";
    private static final String ATE_1 = "Até 1 mês";
    private static final String ATE_2 = "Até 2 meses";
    private static final String ATE_3 = "Até 3 meses";
    private static final String TODOS = "Todos os equipamentos";
    private static final String SEIS_MESES = "6 meses (180 dias)";
    private static final String UM_ANO = "1 ano (365 dias)";

    private final List<AlertaPreventiva> todos = Contexto.ALERTAS.calcular();
    private final ComboBox<String> periodo = new ComboBox<>(FXCollections.observableArrayList(ATIVOS, ATE_1, ATE_2, ATE_3, TODOS));
    private final ComboBox<String> tecnico;
    private final ComboBox<String> equipamento;
    private final ComboBox<String> programacao = new ComboBox<>(FXCollections.observableArrayList("Todas", "Programada", "Sem programação"));
    private final Tabela<AlertaPreventiva> tabela = new Tabela<AlertaPreventiva>()
            .col(Rotulos.de("equipamento"), a -> a.os().getEquipamento(), 120)
            .col(Rotulos.de("modelo"), a -> a.os().getModelo(), 80)
            .col(Rotulos.de("patrimonio"), a -> a.os().getPatrimonio(), 80)
            .col(Rotulos.de("tecnico"), a -> a.os().getTecnico(), 110)
            .col("Última manutenção", a -> Formatos.data(a.ultima()), 100)
            .col("Validade", a -> Formatos.data(a.vencimento()), 90)
            .col("Tempo restante", a -> tempo(a.dias()), 100)
            .colNo("Prioridade", a -> Ui.badgePrioridade(a.prioridade()), 95)
            .colNo("Ação", a -> a.programada()
                    ? (Node) Ui.duasLinhas("Programada", a.osEmAberto().getNumero())
                    : (Node) Ui.pequeno(Ui.botao("Programar", () -> Navegador.ir(Rota.CADASTRO, a.os()))), 105);
    private List<AlertaPreventiva> exibidos = new ArrayList<>();

    public AlertasView() {
        super("Alertas de preventivas e calibrações",
                "Programe-se para renovar as preventivas antes do vencimento e mantenha os equipamentos em conformidade.");
        TreeSet<String> tecnicos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<String> equipamentos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (AlertaPreventiva a : todos) {
            if (a.os().getTecnico() != null) {
                tecnicos.add(a.os().getTecnico());
            }
            if (a.os().getEquipamento() != null) {
                equipamentos.add(a.os().getEquipamento());
            }
        }
        tecnico = Ui.comboFiltro(new ArrayList<>(tecnicos), "Todos");
        equipamento = Ui.comboFiltro(new ArrayList<>(equipamentos), "Todos");
        periodo.setValue(ATIVOS);
        programacao.setValue("Todas");

        getChildren().addAll(aviso(), indicadores(), comoFunciona(),
                Ui.colunas(new double[] { 68, 32 }, cartaoLista(), colunaLateral()));
        filtrar();
    }

    // ------------------------------------------------------------ partes

    private Node aviso() {
        long urgentes = contar(Prioridade.VENCIDA, Prioridade.URGENTE);
        return Ui.faixa(urgentes > 0
                ? "Atenção: existem " + urgentes + (urgentes == 1 ? " preventiva vencida ou vencendo" : " preventivas vencidas ou vencendo")
                + " em até 30 dias. Organize a agenda técnica e programe os atendimentos com antecedência."
                : "Nenhuma preventiva vencida ou próxima do vencimento. Tudo em dia.", urgentes == 0);
    }

    private FlowPane indicadores() {
        FlowPane flow = new FlowPane(12, 12);
        flow.getChildren().addAll(
                Ui.kpi("Vencidas ou em até 1 mês", String.valueOf(contar(Prioridade.VENCIDA, Prioridade.URGENTE)), "alert",
                        "#fbdcdc", "#b42a2a", "Ação imediata"),
                Ui.kpi("Vencendo em até 2 meses", String.valueOf(contar(Prioridade.PROGRAMAR)), "alert",
                        "#fdecd0", "#a8620a", "Programar atendimento"),
                Ui.kpi("Vencendo em até 3 meses", String.valueOf(contar(Prioridade.PLANEJAR)), "alert",
                        "#fdf3c4", "#8a6d00", "Planejar agenda"),
                Ui.kpi("Preventivas em dia", String.valueOf(contar(Prioridade.EM_DIA)), "ok",
                        "#d8f3e4", "#17794a", "Sem urgência"));
        return flow;
    }

    private long contar(Prioridade... prioridades) {
        List<Prioridade> lista = List.of(prioridades);
        return todos.stream().filter(a -> lista.contains(a.prioridade())).count();
    }

    private VBox comoFunciona() {
        HBox passos = new HBox(24,
                passo("cad", "3 meses antes", "Planejar a agenda e verificar a disponibilidade de peças e equipe."),
                passo("hist", "2 meses antes", "Confirmar a programação com o responsável e o setor."),
                passo("alert", "1 mês antes", "Priorizar o atendimento e abrir a OS preventiva."));
        return Ui.card("Como funciona o alerta", passos);
    }

    private HBox passo(String icone, String titulo, String texto) {
        Label t = new Label(titulo);
        t.getStyleClass().add("destaque-texto");
        Label d = new Label(texto);
        d.setWrapText(true);
        d.setMaxWidth(260);
        HBox h = new HBox(10, Icones.criar(icone, 22, "#2f6fe4"), new VBox(2, t, d));
        h.setAlignment(Pos.TOP_LEFT);
        return h;
    }

    private VBox cartaoLista() {
        for (ComboBox<String> c : List.of(periodo, tecnico, equipamento, programacao)) {
            c.valueProperty().addListener((obs, antes, depois) -> filtrar());
        }
        tabela.linhasVisiveis(8);
        HBox filtros = new HBox(10, Ui.campo("Período", periodo), Ui.campo(Rotulos.de("tecnico"), tecnico),
                Ui.campo(Rotulos.de("equipamento"), equipamento), Ui.campo("Programação", programacao));
        return Ui.card("Preventivas próximas do vencimento", filtros, tabela.view(),
                Ui.acoes(Ui.secundario("Exportar lista (CSV)", this::exportar)));
    }

    private VBox colunaLateral() {
        long ativos = todos.stream().filter(AlertaPreventiva::ativo).count();
        long programadas = todos.stream().filter(a -> a.ativo() && a.programada()).count();
        double fracao = ativos == 0 ? 1.0 : (double) programadas / ativos;
        ProgressBar barra = new ProgressBar(fracao);
        barra.setMaxWidth(Double.MAX_VALUE);
        HBox linhaResumo = new HBox(8, new Label(programadas + " de " + ativos + " já programadas"), Ui.espaco(),
                new Label(Math.round(fracao * 100) + "%"));
        VBox resumo = Ui.card("Resumo da programação", linhaResumo, barra);

        VBox proximo = Ui.card("Próximo vencimento");
        todos.stream().filter(a -> a.ativo() && !a.programada()).findFirst().ifPresentOrElse(a -> {
            Label nome = new Label(a.os().getEquipamento());
            nome.getStyleClass().add("destaque-texto");
            proximo.getChildren().addAll(
                    new HBox(8, nome, Ui.badgePrioridade(a.prioridade())),
                    new Label(Rotulos.de("patrimonio") + ": " + Formatos.texto(a.os().getPatrimonio())),
                    new Label("Validade: " + Formatos.data(a.vencimento()) + " (" + tempo(a.dias()) + ")"),
                    new Label(Rotulos.de("tecnico") + ": " + Formatos.texto(a.os().getTecnico())),
                    Ui.botao("Programar agora", () -> Navegador.ir(Rota.CADASTRO, a.os())));
        }, () -> proximo.getChildren().add(new Label("Nenhum vencimento pendente.")));

        return new VBox(16, resumo, proximo, regras());
    }

    private VBox regras() {
        ConfiguracaoService cfg = Contexto.CONFIG;
        Interruptor tres = new Interruptor("Avisar 3 meses antes");
        tres.setSelecionado(cfg.getBool(ConfiguracaoService.ALERTA_3_MESES));
        Interruptor dois = new Interruptor("Avisar 2 meses antes");
        dois.setSelecionado(cfg.getBool(ConfiguracaoService.ALERTA_2_MESES));
        Interruptor um = new Interruptor("Avisar 1 mês antes");
        um.setSelecionado(cfg.getBool(ConfiguracaoService.ALERTA_1_MES));
        ComboBox<String> validade = new ComboBox<>(FXCollections.observableArrayList(SEIS_MESES, UM_ANO));
        validade.setValue(cfg.getInt(ConfiguracaoService.ALERTA_VALIDADE_DIAS, 365) <= 180 ? SEIS_MESES : UM_ANO);

        Runnable salvar = () -> Ui.executar(() -> {
            cfg.salvar(Map.of(
                    ConfiguracaoService.ALERTA_3_MESES, String.valueOf(tres.isSelecionado()),
                    ConfiguracaoService.ALERTA_2_MESES, String.valueOf(dois.isSelecionado()),
                    ConfiguracaoService.ALERTA_1_MES, String.valueOf(um.isSelecionado()),
                    ConfiguracaoService.ALERTA_VALIDADE_DIAS, SEIS_MESES.equals(validade.getValue()) ? "180" : "365"));
            Toast.mostrar("Regras salvas");
            Navegador.ir(Rota.ALERTAS);
        });
        tres.selecionadoProperty().addListener((obs, antes, depois) -> salvar.run());
        dois.selecionadoProperty().addListener((obs, antes, depois) -> salvar.run());
        um.selecionadoProperty().addListener((obs, antes, depois) -> salvar.run());
        validade.valueProperty().addListener((obs, antes, depois) -> salvar.run());

        Label dica = new Label("Quando a OS não define a próxima preventiva, o vencimento é a última manutenção + a validade padrão.");
        dica.setWrapText(true);
        dica.getStyleClass().add("dica-texto");
        return Ui.card("Regras de alerta", tres, dois, um, Ui.campo("Validade padrão", validade), dica);
    }

    // ------------------------------------------------------------ ações

    private void filtrar() {
        String p = periodo.getValue() == null ? ATIVOS : periodo.getValue();
        List<AlertaPreventiva> lista = new ArrayList<>();
        for (AlertaPreventiva a : todos) {
            boolean dentro = switch (p) {
                case TODOS -> true;
                case ATE_1 -> a.dias() <= 30;
                case ATE_2 -> a.dias() <= 60;
                case ATE_3 -> a.dias() <= 90;
                default -> a.ativo();
            };
            boolean tec = tecnico.getValue() == null || tecnico.getValue().equals(a.os().getTecnico());
            boolean equip = equipamento.getValue() == null || equipamento.getValue().equals(a.os().getEquipamento());
            String prog = programacao.getValue();
            boolean programacaoOk = prog == null || prog.equals("Todas") || (prog.equals("Programada") == a.programada());
            if (dentro && tec && equip && programacaoOk) {
                lista.add(a);
            }
        }
        exibidos = lista;
        tabela.itens(lista);
    }

    private void exportar() {
        if (exibidos.isEmpty()) {
            Toast.mostrar("Nada para exportar.");
            return;
        }
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Exportar lista de alertas");
        escolha.setInitialFileName("alertas-preventivas-" + java.time.LocalDate.now() + ".csv");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File destino = escolha.showSaveDialog(Ui.janela(tabela.view()));
        if (destino == null) {
            return;
        }
        List<String[]> linhas = new ArrayList<>();
        linhas.add(new String[] { Rotulos.de("equipamento"), Rotulos.de("modelo"), Rotulos.de("patrimonio"), Rotulos.de("serie"),
                Rotulos.de("tecnico"), "Última manutenção", "Vencimento", "Tempo restante", "Prioridade", "Programação" });
        for (AlertaPreventiva a : exibidos) {
            linhas.add(new String[] { Formatos.texto(a.os().getEquipamento()), Formatos.texto(a.os().getModelo()),
                    Formatos.texto(a.os().getPatrimonio()), Formatos.texto(a.os().getNumeroSerie()),
                    Formatos.texto(a.os().getTecnico()), Formatos.data(a.ultima()), Formatos.data(a.vencimento()),
                    tempo(a.dias()), a.prioridade().getRotulo(),
                    a.programada() ? a.osEmAberto().getNumero() + " em aberto" : "Sem programação" });
        }
        Contexto.IMPORT_EXPORT.gravar(destino.toPath(), Csv.escrever(linhas));
        Toast.mostrar("Lista exportada: " + destino.getName());
    }

    static String tempo(long dias) {
        if (dias < 0) {
            return "vencida há " + (-dias) + (dias == -1 ? " dia" : " dias");
        }
        if (dias <= 30) {
            return dias + (dias == 1 ? " dia" : " dias");
        }
        if (dias < 365) {
            long meses = Math.round(dias / 30.0);
            return meses + (meses == 1 ? " mês" : " meses");
        }
        return String.format("%.1f anos", dias / 365.0).replace('.', ',');
    }
}
