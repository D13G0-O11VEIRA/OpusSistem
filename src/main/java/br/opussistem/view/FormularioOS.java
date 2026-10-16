package br.opussistem.view;

import br.opussistem.controller.Contexto;
import br.opussistem.exception.AppException;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.StatusOS;
import br.opussistem.service.ConfiguracaoService;
import br.opussistem.service.Rotulos;
import br.opussistem.util.Formatos;
import br.opussistem.view.componentes.EditorTexto;
import br.opussistem.view.componentes.Ui;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulário de OS usado no Cadastro e no Alterar OS. Campos obrigatórios (conforme o banco):
 * cliente, equipamento e data de entrada. Os textos de "natureza do serviço", laudo e observação
 * usam o editor de texto formatado; a observação nunca sai no relatório impresso.
 */
public class FormularioOS extends VBox {

    private final boolean edicao;
    private OrdemServico original;

    private final TextField cliente = Ui.texto("Nome do cliente");
    private final TextField telefone = Ui.texto("(00) 00000-0000");
    private final ComboBox<String> equipamento;
    private final TextField modelo = Ui.texto("");
    private final ComboBox<String> fabricante;
    private final TextField patrimonio = Ui.texto("");
    private final TextField serie = Ui.texto("");
    private final ComboBox<String> statusEquipamento;
    private final ComboBox<String> manutencao;
    private final ComboBox<String> tecnico;
    private final DatePicker entrada = Ui.data();
    private final DatePicker previsao = Ui.data();
    private final DatePicker proximaPreventiva = Ui.data();
    private final TextField valorPrevisto = Ui.texto("0,00");
    private final TextField valorFinal = Ui.texto("0,00");
    private final ComboBox<StatusOS> status = new ComboBox<>();
    private final EditorTexto natureza = new EditorTexto(190);
    private final EditorTexto laudo = new EditorTexto(190);
    private final EditorTexto observacoes = new EditorTexto(160);
    private final ListView<String> anexosExistentes = new ListView<>();
    private final List<Path> anexosNovos = new ArrayList<>();
    private final ListView<String> anexosNovosLista = new ListView<>();

    public FormularioOS(boolean edicao) {
        super(16);
        this.edicao = edicao;
        ConfiguracaoService cfg = Contexto.CONFIG;
        equipamento = Ui.comboEditavel(Contexto.OS.sugestoes("equipamento", cfg.lista(ConfiguracaoService.LISTA_EQUIPAMENTOS)));
        fabricante = Ui.comboEditavel(Contexto.OS.sugestoes("fabricante", cfg.lista(ConfiguracaoService.LISTA_FABRICANTES)));
        tecnico = Ui.comboEditavel(Contexto.OS.sugestoes("tecnico_responsavel", cfg.lista(ConfiguracaoService.LISTA_TECNICOS)));
        manutencao = Ui.comboEditavel(Contexto.OS.sugestoes("tipo_manutencao", cfg.lista(ConfiguracaoService.LISTA_MANUTENCAO)));
        statusEquipamento = Ui.comboEditavel(Contexto.OS.sugestoes("status_equipamento", cfg.lista(ConfiguracaoService.LISTA_STATUS_EQUIPAMENTO)));

        Ui.Grade dados = Ui.grade(3);
        dados.adicionar(Ui.campo(Rotulos.de("cliente") + " *", cliente));
        dados.adicionar(Ui.campo("Telefone", telefone));
        dados.adicionar(Ui.campo(Rotulos.de("equipamento") + " *", equipamento));
        dados.adicionar(Ui.campo(Rotulos.de("modelo"), modelo));
        dados.adicionar(Ui.campo(Rotulos.de("fabricante"), fabricante));
        dados.adicionar(Ui.campo(Rotulos.de("patrimonio"), patrimonio));
        dados.adicionar(Ui.campo(Rotulos.de("serie"), serie));
        dados.adicionar(Ui.campo(Rotulos.de("statusEquipamento"), statusEquipamento));
        dados.adicionar(Ui.campo(Rotulos.de("manutencao"), manutencao));
        dados.adicionar(Ui.campo(Rotulos.de("tecnico"), tecnico));
        dados.adicionar(Ui.campo("Data de entrada *", entrada));
        dados.adicionar(Ui.campo("Previsão de entrega", previsao));
        dados.adicionar(Ui.campo("Próxima preventiva", proximaPreventiva));
        dados.adicionar(Ui.campo("Valor previsto (R$)", valorPrevisto));
        dados.adicionar(Ui.campo("Valor final (R$)", valorFinal));
        if (edicao) {
            status.setPromptText("Status da OS");
            dados.adicionar(Ui.campo("Status da OS", status));
        }

        Button adicionar = Ui.secundario("Adicionar anexos...", this::escolherAnexos);
        Label dica = new Label("PDF, JPG ou PNG, até 10 MB cada. Ficam em storage/ordens/<número da OS>.");
        dica.getStyleClass().add("dica-texto");
        anexosExistentes.setPrefHeight(70);
        anexosNovosLista.setPrefHeight(70);
        VBox anexos = new VBox(8);
        if (edicao) {
            anexos.getChildren().add(Ui.campo("Anexos já salvos", anexosExistentes));
        }
        anexos.getChildren().addAll(Ui.campo("Anexos a salvar", anexosNovosLista), Ui.linha(adicionar), dica);

        Label secaoDetalhes = new Label("Detalhes da solicitação");
        secaoDetalhes.getStyleClass().add("card-titulo");
        VBox detalhes = new VBox(12, secaoDetalhes,
                Ui.campo(Rotulos.de("natureza"), natureza),
                Ui.campo(Rotulos.de("laudo"), laudo),
                Ui.campo(Rotulos.de("observacoes") + " (visível só no sistema, não sai no relatório impresso)", observacoes));
        VBox.setMargin(secaoDetalhes, new javafx.geometry.Insets(10, 0, 0, 0));

        Label secaoAnexos = new Label("Anexos (opcional)");
        secaoAnexos.getStyleClass().add("card-titulo");
        VBox.setMargin(secaoAnexos, new javafx.geometry.Insets(10, 0, 0, 0));

        getChildren().add(Ui.card("Dados do equipamento", dados, detalhes, secaoAnexos, anexos));
        preencher(valoresPadrao());
    }

    /** Valores iniciais de uma nova OS, vindos de Perfil > Padrões da OS. */
    public static OrdemServico valoresPadrao() {
        ConfiguracaoService c = Contexto.CONFIG;
        OrdemServico os = new OrdemServico();
        os.setEquipamento(vazioParaNulo(c.get(ConfiguracaoService.PAD_EQUIPAMENTO)));
        os.setFabricante(vazioParaNulo(c.get(ConfiguracaoService.PAD_FABRICANTE)));
        String tecnicoPadrao = c.get(ConfiguracaoService.PAD_TECNICO);
        os.setTecnico(tecnicoPadrao.isBlank() ? Contexto.usuario().getNome() : tecnicoPadrao);
        os.setTipoManutencao(vazioParaNulo(c.get(ConfiguracaoService.PAD_MANUTENCAO)));
        os.setStatusEquipamento(vazioParaNulo(c.get(ConfiguracaoService.PAD_STATUS_EQUIPAMENTO)));
        os.setDataEntrada(LocalDate.now().atStartOfDay());
        int dias = c.getInt(ConfiguracaoService.PAD_DIAS_PREVENTIVA, 0);
        os.setProximaPreventiva(dias > 0 ? LocalDate.now().plusDays(dias) : null);
        os.setDescricaoProblema(vazioParaNulo(c.get(ConfiguracaoService.PAD_NATUREZA)));
        os.setLaudoTecnico(vazioParaNulo(c.get(ConfiguracaoService.PAD_LAUDO)));
        os.setObservacoes(vazioParaNulo(c.get(ConfiguracaoService.PAD_OBSERVACOES)));
        return os;
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    // ------------------------------------------------------------ preencher / coletar

    public void preencher(OrdemServico os) {
        this.original = os.getId() == null ? null : os;
        cliente.setText(Formatos.texto(os.getCliente()));
        telefone.setText(Formatos.texto(os.getTelefone()));
        Ui.definir(equipamento, os.getEquipamento());
        modelo.setText(Formatos.texto(os.getModelo()));
        Ui.definir(fabricante, os.getFabricante());
        patrimonio.setText(Formatos.texto(os.getPatrimonio()));
        serie.setText(Formatos.texto(os.getNumeroSerie()));
        Ui.definir(statusEquipamento, os.getStatusEquipamento());
        Ui.definir(manutencao, os.getTipoManutencao());
        Ui.definir(tecnico, os.getTecnico());
        entrada.setValue(os.getDataEntrada() == null ? null : os.getDataEntrada().toLocalDate());
        previsao.setValue(os.getPrevisaoEntrega() == null ? null : os.getPrevisaoEntrega().toLocalDate());
        proximaPreventiva.setValue(os.getProximaPreventiva());
        valorPrevisto.setText(Formatos.decimal(os.getValorPrevisto()));
        valorFinal.setText(Formatos.decimal(os.getValorFinal()));
        natureza.definir(os.getDescricaoProblema());
        laudo.definir(os.getLaudoTecnico());
        observacoes.definir(os.getObservacoes());
        anexosNovos.clear();
        anexosNovosLista.getItems().clear();
        if (edicao) {
            List<StatusOS> opcoes = new ArrayList<>(List.of(StatusOS.ABERTA, StatusOS.EM_ANDAMENTO, StatusOS.AGUARDANDO_PECA, StatusOS.FECHADA));
            if (os.getStatus() == StatusOS.REABERTA) {
                opcoes.add(1, StatusOS.REABERTA);
            }
            status.setItems(FXCollections.observableArrayList(opcoes));
            status.setValue(os.getStatus());
            anexosExistentes.getItems().clear();
            if (os.getId() != null) {
                for (Path p : Contexto.OS.listarAnexos(os)) {
                    anexosExistentes.getItems().add(p.getFileName().toString());
                }
            }
        }
    }

    /** Lê os campos e monta a OS (sem gravar). Lança AppException se algum valor for inválido. */
    public OrdemServico coletar() {
        OrdemServico os = new OrdemServico();
        if (original != null) {
            os.setId(original.getId());
            os.setNumero(original.getNumero());
            os.setStatus(original.getStatus());
        }
        os.setCliente(Ui.valor(cliente));
        os.setTelefone(Ui.valor(telefone));
        os.setEquipamento(Ui.valor(equipamento));
        os.setModelo(Ui.valor(modelo));
        os.setFabricante(Ui.valor(fabricante));
        os.setPatrimonio(Ui.valor(patrimonio));
        os.setNumeroSerie(Ui.valor(serie));
        os.setStatusEquipamento(Ui.valor(statusEquipamento));
        os.setTipoManutencao(Ui.valor(manutencao));
        os.setTecnico(Ui.valor(tecnico));

        LocalDate dataEntrada = entrada.getValue();
        if (dataEntrada != null) {
            // preserva a hora original se a data não foi alterada
            boolean mesmaData = original != null && original.getDataEntrada() != null
                    && original.getDataEntrada().toLocalDate().equals(dataEntrada);
            os.setDataEntrada(mesmaData ? original.getDataEntrada() : dataEntrada.atStartOfDay());
        }
        os.setPrevisaoEntrega(previsao.getValue() == null ? null : previsao.getValue().atStartOfDay());
        os.setProximaPreventiva(proximaPreventiva.getValue());
        os.setValorPrevisto(lerValor(valorPrevisto, "Valor previsto"));
        os.setValorFinal(lerValor(valorFinal, "Valor final"));
        os.setDescricaoProblema(natureza.valor());
        os.setLaudoTecnico(laudo.valor());
        os.setObservacoes(observacoes.valor());
        if (edicao && status.getValue() != null) {
            os.setStatus(status.getValue());
        }
        return os;
    }

    private static BigDecimal lerValor(TextField campo, String nome) {
        String texto = Ui.valor(campo);
        if (texto == null) {
            return null;
        }
        BigDecimal v = Formatos.lerDecimal(texto);
        if (v == null) {
            throw new AppException(nome + " inválido: use números, por exemplo 1.250,00.");
        }
        return v;
    }

    public List<Path> anexosSelecionados() {
        return List.copyOf(anexosNovos);
    }

    private void escolherAnexos() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Escolher anexos");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF e imagens", "*.pdf", "*.jpg", "*.jpeg", "*.png"));
        List<File> arquivos = escolha.showOpenMultipleDialog(Ui.janela(this));
        if (arquivos != null) {
            for (File f : arquivos) {
                anexosNovos.add(f.toPath());
                anexosNovosLista.getItems().add(f.getName());
            }
        }
    }
}
