package br.opussistem.view;

import br.opussistem.controller.Contexto;
import br.opussistem.model.FiltroOS;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.StatusOS;
import br.opussistem.service.Rotulos;
import br.opussistem.util.Formatos;
import br.opussistem.view.componentes.Tabela;
import br.opussistem.view.componentes.Toast;
import br.opussistem.view.componentes.Ui;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

/** Reabertura de OS fechadas, sempre com o motivo registrado no histórico. */
public class ReabrirView extends Tela {

    private final VBox painelMotivo = new VBox();
    private final TextField busca = Ui.texto("Número da OS, cliente ou patrimônio");
    private final Tabela<OrdemServico> tabela = new Tabela<OrdemServico>()
            .col("Nº OS", OrdemServico::getNumero, 110)
            .colNo(Rotulos.de("equipamento"), o -> Ui.duasLinhas(o.getEquipamento(),
                    Formatos.texto(o.getFabricante()) + " | " + Rotulos.de("patrimonio") + ": " + Formatos.texto(o.getPatrimonio())), 220)
            .col(Rotulos.de("cliente"), OrdemServico::getCliente, 150)
            .col(Rotulos.de("tecnico"), OrdemServico::getTecnico, 130)
            .col("Fechada em", o -> Formatos.data(o.getDataEntrega()), 100)
            .colNo("Ações", o -> Ui.pequeno(Ui.perigo("Reabrir", () -> pedirMotivo(o))), 100);

    public ReabrirView(Long idInicial) {
        super("Reabrir OS", "Reabra uma OS fechada informando o motivo. O histórico é preservado; as assinaturas anteriores são removidas.");
        busca.setOnAction(e -> Ui.executar(this::pesquisar));
        tabela.linhasVisiveis(8);
        Label dica = new Label("Só aparecem OS com status Fechada.");
        dica.getStyleClass().add("dica-texto");
        getChildren().addAll(painelMotivo,
                Ui.card(null, Ui.linha(busca, Ui.botao("Pesquisar", this::pesquisar)), dica),
                Ui.card("OS fechadas", tabela.view()));
        javafx.scene.layout.HBox.setHgrow(busca, javafx.scene.layout.Priority.ALWAYS);
        pesquisar();
        if (idInicial != null) {
            Contexto.OS.buscar(idInicial).ifPresent(os -> {
                if (os.getStatus() == StatusOS.FECHADA) {
                    pedirMotivo(os);
                } else {
                    Toast.mostrar("A OS " + os.getNumero() + " não está fechada.");
                }
            });
        }
    }

    private void pesquisar() {
        String t = busca.getText() == null ? "" : busca.getText().trim();
        java.util.LinkedHashSet<OrdemServico> achadas = new java.util.LinkedHashSet<>();
        if (t.isEmpty()) {
            achadas.addAll(Contexto.OS.pesquisar(new FiltroOS().somenteFechadas(true), 100));
        } else {
            achadas.addAll(Contexto.OS.pesquisar(new FiltroOS().somenteFechadas(true).numero(t), 100));
            achadas.addAll(Contexto.OS.pesquisar(new FiltroOS().somenteFechadas(true).cliente(t), 100));
            achadas.addAll(Contexto.OS.pesquisar(new FiltroOS().somenteFechadas(true).patrimonio(t), 100));
        }
        tabela.itens(List.copyOf(achadas));
    }

    /** Mostra, no topo da tela, o cartão para informar o motivo da reabertura. */
    private void pedirMotivo(OrdemServico os) {
        TextArea motivo = new TextArea();
        motivo.setPrefRowCount(3);
        motivo.setWrapText(true);
        VBox card = Ui.card("Reabrir " + os.getNumero(),
                new Label(Formatos.texto(os.getEquipamento()) + " — " + Formatos.texto(os.getCliente())),
                Ui.campo("Motivo da reabertura *", motivo),
                Ui.acoes(Ui.secundario("Cancelar", () -> painelMotivo.getChildren().clear()),
                        Ui.perigo("Reabrir OS", () -> {
                            Contexto.OS.reabrir(os.getId(), motivo.getText(), Contexto.usuario());
                            Toast.mostrar("OS " + os.getNumero() + " reaberta");
                            painelMotivo.getChildren().clear();
                            pesquisar();
                        })));
        painelMotivo.getChildren().setAll(card);
        motivo.requestFocus();
    }
}
