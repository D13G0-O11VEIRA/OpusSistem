package br.opussistem.model;

import java.time.LocalDate;

/**
 * Situação de preventiva de um equipamento.
 * @param os            OS mais recente do equipamento
 * @param ultima        data da última manutenção concluída
 * @param vencimento    data em que a próxima preventiva vence
 * @param dias          dias até o vencimento (negativo = vencida)
 * @param prioridade    prioridade já considerando as regras de alerta ativas
 * @param osEmAberto    OS em aberto do equipamento (já programada), ou null
 */
public record AlertaPreventiva(OrdemServico os, LocalDate ultima, LocalDate vencimento, long dias,
                               Prioridade prioridade, OrdemServico osEmAberto) {

    public boolean programada() {
        return osEmAberto != null;
    }

    public boolean ativo() {
        return prioridade != Prioridade.EM_DIA;
    }
}
