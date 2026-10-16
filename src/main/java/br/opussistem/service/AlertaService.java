package br.opussistem.service;

import br.opussistem.dao.OrdemServicoDAO;
import br.opussistem.model.AlertaPreventiva;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.Prioridade;
import br.opussistem.model.StatusOS;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Alertas de preventiva por equipamento. O equipamento é identificado por patrimônio +
 * nº de série; o vencimento vem da "próxima preventiva" da OS mais recente ou, se vazia,
 * da última manutenção concluída + validade padrão (6 meses ou 1 ano).
 */
public class AlertaService {

    private final OrdemServicoDAO osDao = new OrdemServicoDAO();
    private final ConfiguracaoService config = ConfiguracaoService.instancia();

    public List<AlertaPreventiva> calcular() {
        return calcular(osDao.listarParaAlertas(), LocalDate.now(),
                config.getBool(ConfiguracaoService.ALERTA_1_MES),
                config.getBool(ConfiguracaoService.ALERTA_2_MESES),
                config.getBool(ConfiguracaoService.ALERTA_3_MESES),
                config.getInt(ConfiguracaoService.ALERTA_VALIDADE_DIAS, 365));
    }

    /** Quantidade de equipamentos com alerta ativo (usada no selo do menu). */
    public int quantidadeAtiva() {
        return (int) calcular().stream().filter(AlertaPreventiva::ativo).count();
    }

    /** Versão pura (sem banco), usada também nos testes. */
    static List<AlertaPreventiva> calcular(List<OrdemServico> todas, LocalDate hoje,
                                           boolean m1, boolean m2, boolean m3, int validadePadraoDias) {
        Map<String, List<OrdemServico>> grupos = new LinkedHashMap<>();
        for (OrdemServico os : todas) {
            grupos.computeIfAbsent(chaveEquipamento(os), k -> new ArrayList<>()).add(os);
        }
        List<AlertaPreventiva> alertas = new ArrayList<>();
        for (List<OrdemServico> grupo : grupos.values()) {
            grupo.sort(Comparator.comparing(OrdemServico::dataReferencia, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(OrdemServico::getId).reversed());
            OrdemServico maisRecente = grupo.get(0);
            OrdemServico ultimaFechada = grupo.stream().filter(o -> o.getStatus() == StatusOS.FECHADA).findFirst().orElse(null);
            LocalDate ultima = (ultimaFechada != null ? ultimaFechada.dataReferencia() : maisRecente.getDataEntrada()).toLocalDate();
            LocalDate vencimento = maisRecente.getProximaPreventiva() != null
                    ? maisRecente.getProximaPreventiva() : ultima.plusDays(validadePadraoDias);
            long dias = ChronoUnit.DAYS.between(hoje, vencimento);
            OrdemServico emAberto = grupo.stream().filter(o -> o.getStatus().estaAberta()).findFirst().orElse(null);
            alertas.add(new AlertaPreventiva(maisRecente, ultima, vencimento, dias,
                    prioridade(dias, m1, m2, m3), emAberto));
        }
        alertas.sort(Comparator.comparingLong(AlertaPreventiva::dias));
        return alertas;
    }

    static Prioridade prioridade(long dias, boolean m1, boolean m2, boolean m3) {
        if (dias < 0) {
            return Prioridade.VENCIDA;
        }
        if (dias <= 30) {
            return m1 ? Prioridade.URGENTE : Prioridade.EM_DIA;
        }
        if (dias <= 60) {
            return m2 ? Prioridade.PROGRAMAR : Prioridade.EM_DIA;
        }
        if (dias <= 90) {
            return m3 ? Prioridade.PLANEJAR : Prioridade.EM_DIA;
        }
        return Prioridade.EM_DIA;
    }

    static String chaveEquipamento(OrdemServico os) {
        String pat = os.getPatrimonio() == null ? "" : os.getPatrimonio().trim();
        String serie = os.getNumeroSerie() == null ? "" : os.getNumeroSerie().trim();
        if (pat.isEmpty() && serie.isEmpty()) {
            return "#" + os.getId();
        }
        return pat + "|" + serie;
    }
}
