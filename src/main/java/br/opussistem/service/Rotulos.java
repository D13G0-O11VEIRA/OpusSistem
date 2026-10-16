package br.opussistem.service;

import java.util.Map;

/** Nomes exibidos dos campos; podem ser renomeados em Perfil (ex.: Patrimônio -> Placa). */
public final class Rotulos {

    private static final Map<String, String> PADRAO = Map.ofEntries(
            Map.entry("cliente", "Cliente"),
            Map.entry("equipamento", "Equipamento"),
            Map.entry("modelo", "Modelo"),
            Map.entry("fabricante", "Fabricante"),
            Map.entry("patrimonio", "Patrimônio"),
            Map.entry("serie", "Nº de série"),
            Map.entry("statusEquipamento", "Status do equipamento"),
            Map.entry("manutencao", "Tipo de manutenção"),
            Map.entry("tecnico", "Técnico responsável"),
            Map.entry("natureza", "Natureza do serviço"),
            Map.entry("laudo", "Laudo técnico"),
            Map.entry("observacoes", "Observação"),
            Map.entry("valor", "Valor"));

    private Rotulos() {
    }

    public static Map<String, String> padroes() {
        return PADRAO;
    }

    public static String de(String campo) {
        String personalizado = ConfiguracaoService.instancia().get("rotulo." + campo);
        if (!personalizado.isBlank()) {
            return personalizado;
        }
        return PADRAO.getOrDefault(campo, campo);
    }
}
