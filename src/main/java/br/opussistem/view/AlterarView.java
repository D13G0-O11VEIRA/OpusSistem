package br.opussistem.view;

import br.opussistem.controller.Contexto;
import br.opussistem.controller.Navegador;
import br.opussistem.controller.Rota;
import br.opussistem.model.FiltroOS;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.StatusOS;
import br.opussistem.service.Rotulos;
import br.opussistem.util.Formatos;
import br.opussistem.view.componentes.Tabela;
import br.opussistem.view.componentes.Toast;
import br.opussistem.view.componentes.Ui;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Escolhe uma OS e edita dados, status e andamento. OS fechadas só podem ser reabertas. */
public class AlterarView extends Tela {

    private final StackPane area = new StackPane();
    private final TextField numero = Ui.texto("OS-2026-0001");
    private final TextField cliente = Ui.texto("");
    private final TextField patrimonio = Ui.texto("");
    private final TextField serie = Ui.texto("");
    private final ComboBox<StatusOS> status = new ComboBox<>(FXCollections.observableArrayList(StatusOS.values()));
    private final Tabela<OrdemServico> resultados = new Tabela<OrdemServico>()
            .col("Nº OS", OrdemServico::getNumero, 110)
            .colNo(Rotulos.de("equipamento"), o -> Ui.duasLinhas(o.getEquipamento(),
                    Formatos.texto(o.getFabricante()) + " | " + Rotulos.de("patrimonio") + ": " + Formatos.texto(o.getPatrimonio())), 210)
            .col(Rotulos.de("cliente"), OrdemServico::getCliente, 150)
            .colNo("Recebimento", o -> Ui.duasLinhas(Formatos.data(o.getDataEntrada()), Formatos.ha(o.getDataEntrada())), 110)
            .colNo("Status", o -> Ui.badgeStatus(o.getStatus()), 120)
            .colNo("Ações", o -> Ui.pequeno(Ui.botao("Alterar", () -> abrir(o))), 90);

    public AlterarView(Long idInicial) {
        super("Alterar OS", "Escolha uma OS para editar dados, status e andamento. Cada alteração fica registrada no histórico.");
        getChildren().add(area);
        if (idInicial != null) {
            Contexto.OS.buscar(idInicial).ifPresentOrElse(this::abrir, this::mostrarBusca);
        } else {
            mostrarBusca();
        }
    }

    private void mostrarBusca() {
        status.setPromptText("Todos");
        Ui.Grade filtros = Ui.grade(5);
        filtros.adicionar(Ui.campo("Nº OS", numero)).adicionar(Ui.campo(Rotulos.de("cliente"), cliente))
                .adicionar(Ui.campo(Rotulos.de("patrimonio"), patrimonio)).adicionar(Ui.campo(Rotulos.de("serie"), serie))
                .adicionar(Ui.campo("Status", status));
        for (TextField t : List.of(numero, cliente, patrimonio, serie)) {
            t.setOnAction(e -> Ui.executar(this::pesquisar));
        }
        resultados.linhasVisiveis(8);
        VBox busca = Ui.card(null, filtros, Ui.linha(Ui.botao("Pesquisar", this::pesquisar)));
        VBox lista = Ui.card("OS encontradas", resultados.view());
        area.getChildren().setAll(new VBox(16, busca, lista));
        pesquisar();
    }

    private void pesquisar() {
        FiltroOS f = new FiltroOS().numero(numero.getText()).cliente(cliente.getText())
                .patrimonio(patrimonio.getText()).serie(serie.getText()).status(status.getValue());
        resultados.itens(Contexto.OS.pesquisar(f, 60));
    }

    private void abrir(OrdemServico os) {
        OrdemServico atual = Contexto.OS.buscar(os.getId()).orElse(os);
        if (atual.getStatus() == StatusOS.FECHADA) {
            Label aviso = new Label("Uma OS fechada não pode ser alterada. Reabra a OS para editar os dados "
                    + "(as assinaturas anteriores serão removidas).");
            aviso.setWrapText(true);
            area.getChildren().setAll(Ui.card("A OS " + atual.getNumero() + " está fechada", aviso,
                    Ui.linha(Ui.botao("Reabrir OS", () -> Navegador.ir(Rota.REABRIR, atual.getId())),
                            Ui.secundario("Escolher outra OS", this::mostrarBusca))));
            return;
        }
        FormularioOS form = new FormularioOS(true);
        form.preencher(atual);
        TextField nota = Ui.texto("O que foi feito (opcional) — aparece no histórico");
        VBox tela = new VBox(16,
                Ui.card("OS " + atual.getNumero() + " — " + Formatos.texto(atual.getEquipamento()),
                        new Label(Rotulos.de("cliente") + ": " + Formatos.texto(atual.getCliente()))),
                form,
                Ui.card("Registro da alteração", nota),
                Ui.acoes(Ui.secundario("Cancelar", this::mostrarBusca),
                        Ui.botao("Salvar alterações", () -> {
                            OrdemServico salva = Contexto.OS.atualizar(form.coletar(), nota.getText(), Contexto.usuario());
                            if (!form.anexosSelecionados().isEmpty()) {
                                Contexto.OS.adicionarAnexos(salva, form.anexosSelecionados());
                            }
                            Toast.mostrar("OS " + salva.getNumero() + " atualizada");
                            Navegador.ir(Rota.HISTORICO, salva.getId());
                        })));
        area.getChildren().setAll(tela);
    }
}
