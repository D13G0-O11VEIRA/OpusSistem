package br.opussistem.view;

import br.opussistem.controller.Contexto;
import br.opussistem.controller.Navegador;
import br.opussistem.controller.Rota;
import br.opussistem.model.OrdemServico;
import br.opussistem.view.componentes.Toast;
import br.opussistem.view.componentes.Ui;

/** Abertura de uma nova OS. Pode receber uma OS "base" (ex.: vinda dos Alertas de preventivas). */
public class CadastroView extends Tela {

    public CadastroView(OrdemServico base) {
        super("Nova Ordem de Serviço", "Preencha os dados para abrir uma nova ordem de serviço.");
        FormularioOS form = new FormularioOS(false);
        if (base != null) {
            OrdemServico preenchida = FormularioOS.valoresPadrao();
            preenchida.setCliente(base.getCliente());
            preenchida.setTelefone(base.getTelefone());
            preenchida.setEquipamento(base.getEquipamento());
            preenchida.setModelo(base.getModelo());
            preenchida.setFabricante(base.getFabricante());
            preenchida.setPatrimonio(base.getPatrimonio());
            preenchida.setNumeroSerie(base.getNumeroSerie());
            preenchida.setStatusEquipamento(base.getStatusEquipamento());
            if (base.getTecnico() != null) {
                preenchida.setTecnico(base.getTecnico());
            }
            preenchida.setTipoManutencao(base.getTipoManutencao());
            form.preencher(preenchida);
        }
        getChildren().addAll(form, Ui.acoes(
                Ui.secundario("Limpar", () -> form.preencher(FormularioOS.valoresPadrao())),
                Ui.botao("Salvar e abrir OS", () -> salvar(form))));
    }

    private void salvar(FormularioOS form) {
        OrdemServico nova = Contexto.OS.criar(form.coletar(), Contexto.usuario());
        if (!form.anexosSelecionados().isEmpty()) {
            Contexto.OS.adicionarAnexos(nova, form.anexosSelecionados());
        }
        Toast.mostrar("OS " + nova.getNumero() + " aberta com sucesso");
        Navegador.ir(Rota.HISTORICO, nova.getId());
    }
}
