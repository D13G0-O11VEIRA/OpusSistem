package br.opussistem.view;

import br.opussistem.controller.Contexto;
import br.opussistem.exception.AppException;
import br.opussistem.model.FiltroOS;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.StatusOS;
import br.opussistem.service.ImportExportService;
import br.opussistem.service.ImportExportService.ArquivoImportacao;
import br.opussistem.service.ImportExportService.Campo;
import br.opussistem.service.ImportExportService.ModoConflito;
import br.opussistem.service.ImportExportService.ResultadoImportacao;
import br.opussistem.service.Rotulos;
import br.opussistem.util.Formatos;
import br.opussistem.view.componentes.Dialogos;
import br.opussistem.view.componentes.Tabela;
import br.opussistem.view.componentes.Toast;
import br.opussistem.view.componentes.Ui;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Exporta OS (CSV para Excel e outros sistemas, JSON para backup) e importa arquivos de outros
 * softwares de OS/oficina. Na importação, a correspondência entre as colunas do arquivo e os campos
 * do OpusSistem é sugerida automaticamente e pode ser ajustada antes de importar.
 */
public class ImportarExportarView extends Tela {

    private static final String CSV = "CSV (Excel e outros sistemas)";
    private static final String JSON = "JSON (backup do OpusSistem)";
    private static final String IGNORAR_COLUNA = "— ignorar —";

    private final ImportExportService servico = Contexto.IMPORT_EXPORT;

    // exportação
    private final ComboBox<StatusOS> exportStatus = new ComboBox<>(FXCollections.observableArrayList(StatusOS.values()));
    private final DatePicker exportInicio = Ui.data();
    private final DatePicker exportFim = Ui.data();
    private final ComboBox<String> formato = new ComboBox<>(FXCollections.observableArrayList(CSV, JSON));

    // importação
    private final VBox areaImportacao = new VBox(12);
    private final Map<Campo, ComboBox<String>> combos = new EnumMap<>(Campo.class);
    private final ComboBox<StatusOS> statusPadrao = new ComboBox<>(FXCollections.observableArrayList(StatusOS.values()));
    private final ComboBox<String> conflito = new ComboBox<>(FXCollections.observableArrayList(
            "Ignorar a OS que já existe", "Atualizar a OS existente", "Criar como uma nova OS"));
    private final Label infoArquivo = new Label("Nenhum arquivo escolhido.");
    private final Tabela<OrdemServico> previa = new Tabela<OrdemServico>()
            .col("Nº", OrdemServico::getNumero, 110)
            .col(Rotulos.de("cliente"), OrdemServico::getCliente, 150)
            .col(Rotulos.de("equipamento"), OrdemServico::getEquipamento, 150)
            .col(Rotulos.de("patrimonio"), OrdemServico::getPatrimonio, 90)
            .col("Entrada", o -> Formatos.data(o.getDataEntrada()), 90)
            .col("Status", o -> o.getStatus().getRotulo(), 110);
    private ArquivoImportacao arquivo;
    private File origem;

    public ImportarExportarView() {
        super("Exportar / Importar", "Faça backup, exporte para planilhas e outros sistemas ou importe OS de outros softwares.");
        exportStatus.setPromptText("Todos os status");
        formato.setValue(CSV);
        statusPadrao.setValue(StatusOS.ABERTA);
        conflito.setValue("Ignorar a OS que já existe");

        VBox exportar = Ui.card("Exportar",
                Ui.linha(Ui.campo("Status", exportStatus), Ui.campo("Entrada de", exportInicio), Ui.campo("até", exportFim)),
                Ui.campo("Formato", formato),
                Ui.linha(Ui.botao("Exportar...", this::exportar), Ui.secundario("Salvar modelo CSV vazio...", this::salvarModelo)));
        exportar.setPrefWidth(10);
        HBox.setHgrow(exportar, Priority.ALWAYS);

        Label dica = new Label("Aceita CSV (separado por ; , tab ou |) e JSON. Arquivos do Excel: use \"Salvar como CSV\". "
                + "Funciona com arquivos de outros sistemas: você confere a correspondência das colunas antes de importar.");
        dica.setWrapText(true);
        dica.getStyleClass().add("dica-texto");
        VBox importar = Ui.card("Importar", dica,
                Ui.linha(Ui.botao("Escolher arquivo...", this::escolherArquivo), infoArquivo), areaImportacao);
        importar.setPrefWidth(10);
        HBox.setHgrow(importar, Priority.ALWAYS);

        getChildren().add(Ui.colunas(new double[] { 38, 62 }, exportar, importar));
    }

    // ------------------------------------------------------------ exportação

    private void exportar() {
        FiltroOS f = new FiltroOS().status(exportStatus.getValue()).periodo(exportInicio.getValue(), exportFim.getValue());
        List<OrdemServico> lista = new ArrayList<>(Contexto.OS.pesquisar(f, 100_000));
        if (lista.isEmpty()) {
            throw new AppException("Nenhuma OS corresponde aos filtros.");
        }
        java.util.Collections.reverse(lista); // da mais antiga para a mais recente
        boolean json = JSON.equals(formato.getValue());
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Salvar exportação");
        escolha.setInitialFileName("ordens-de-servico-" + LocalDate.now() + (json ? ".json" : ".csv"));
        escolha.getExtensionFilters().add(json ? new FileChooser.ExtensionFilter("JSON", "*.json")
                : new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File destino = escolha.showSaveDialog(Ui.janela(infoArquivo));
        if (destino == null) {
            return;
        }
        servico.gravar(destino.toPath(), json ? servico.paraJson(lista, null) : servico.paraCsv(lista));
        Toast.mostrar(lista.size() + " OS exportadas para " + destino.getName());
    }

    private void salvarModelo() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Salvar modelo de importação");
        escolha.setInitialFileName("modelo-importacao.csv");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File destino = escolha.showSaveDialog(Ui.janela(infoArquivo));
        if (destino != null) {
            servico.gravar(destino.toPath(), servico.modeloCsv());
            Toast.mostrar("Modelo salvo.");
        }
    }

    // ------------------------------------------------------------ importação

    private void escolherArquivo() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Escolher arquivo para importar");
        escolha.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV ou JSON", "*.csv", "*.json", "*.txt"),
                new FileChooser.ExtensionFilter("Todos os arquivos", "*.*"));
        File escolhido = escolha.showOpenDialog(Ui.janela(infoArquivo));
        if (escolhido == null) {
            return;
        }
        arquivo = servico.ler(escolhido.toPath());
        origem = escolhido;
        infoArquivo.setText(escolhido.getName() + " — " + arquivo.linhas().size() + " linhas, " + arquivo.colunas().size() + " colunas");
        montarMapeamento();
    }

    private void montarMapeamento() {
        areaImportacao.getChildren().clear();
        combos.clear();
        Map<Campo, String> sugestao = servico.sugerirMapeamento(arquivo.colunas());
        List<String> opcoes = new ArrayList<>();
        opcoes.add(IGNORAR_COLUNA);
        opcoes.addAll(arquivo.colunas());

        Ui.Grade grade = Ui.grade(2);
        for (Campo campo : Campo.values()) {
            ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(opcoes));
            combo.setValue(sugestao.getOrDefault(campo, IGNORAR_COLUNA));
            combo.valueProperty().addListener((obs, antes, depois) -> atualizarPrevia());
            combos.put(campo, combo);
            grade.adicionar(Ui.campo(campo.rotulo(), combo));
        }
        Label titulo = new Label("Correspondência das colunas (confira antes de importar)");
        titulo.getStyleClass().add("card-titulo");
        previa.view().setPrefHeight(180);
        areaImportacao.getChildren().addAll(titulo, grade,
                Ui.linha(Ui.campo("Status quando não informado", statusPadrao), Ui.campo("Se a OS já existir (mesmo número)", conflito)),
                new Label("Prévia das primeiras linhas:"), previa.view(),
                Ui.acoes(Ui.botao("Importar " + arquivo.linhas().size() + " OS", this::importar)));
        atualizarPrevia();
    }

    private Map<Campo, String> mapeamentoEscolhido() {
        Map<Campo, String> mapa = new EnumMap<>(Campo.class);
        for (Map.Entry<Campo, ComboBox<String>> e : combos.entrySet()) {
            String v = e.getValue().getValue();
            if (v != null && !IGNORAR_COLUNA.equals(v)) {
                mapa.put(e.getKey(), v);
            }
        }
        return mapa;
    }

    private void atualizarPrevia() {
        if (arquivo == null) {
            return;
        }
        List<Map<String, String>> primeiras = arquivo.linhas().subList(0, Math.min(5, arquivo.linhas().size()));
        ArquivoImportacao amostra = new ArquivoImportacao(arquivo.nome(), arquivo.colunas(), primeiras, arquivo.formatoNativo());
        previa.itens(servico.converter(amostra, mapeamentoEscolhido(), statusPadrao.getValue()));
    }

    private void importar() {
        if (arquivo == null) {
            return;
        }
        List<OrdemServico> lista = servico.converter(arquivo, mapeamentoEscolhido(), statusPadrao.getValue());
        ModoConflito modo = switch (conflito.getSelectionModel().getSelectedIndex()) {
            case 1 -> ModoConflito.ATUALIZAR;
            case 2 -> ModoConflito.CRIAR_NOVA;
            default -> ModoConflito.IGNORAR;
        };
        if (!Dialogos.confirmar("Importar " + lista.size() + " OS de " + origem.getName() + "?")) {
            return;
        }
        ResultadoImportacao r = servico.importar(lista, modo, Contexto.usuario(), origem.getName());
        StringBuilder texto = new StringBuilder();
        texto.append(r.criadas()).append(" OS criadas, ").append(r.atualizadas()).append(" atualizadas, ")
                .append(r.ignoradas()).append(" ignoradas.");
        if (!r.erros().isEmpty()) {
            texto.append("\n\nProblemas (").append(r.erros().size()).append("):");
            r.erros().stream().limit(8).forEach(e -> texto.append("\n• ").append(e));
        }
        Dialogos.info(texto.toString());
        areaImportacao.getChildren().clear();
        infoArquivo.setText("Nenhum arquivo escolhido.");
        arquivo = null;
    }
}
