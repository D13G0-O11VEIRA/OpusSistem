package br.opussistem.service;

import br.opussistem.dao.HistoricoDAO;
import br.opussistem.dao.OrdemServicoDAO;
import br.opussistem.model.AlertaPreventiva;
import br.opussistem.model.EventoHistorico;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.ResumoMensal;
import br.opussistem.model.ResumoPainel;
import br.opussistem.model.StatusOS;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Dados do dashboard. */
public class DashboardService {

    private final OrdemServicoDAO osDao = new OrdemServicoDAO();
    private final HistoricoDAO historicoDao = new HistoricoDAO();
    private final AlertaService alertaService;

    public DashboardService(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    public ResumoPainel resumo() {
        Map<StatusOS, Integer> por = osDao.contarPorStatus();
        int total = por.values().stream().mapToInt(Integer::intValue).sum();
        int abertas = por.get(StatusOS.ABERTA) + por.get(StatusOS.EM_ANDAMENTO);
        int proximas = (int) alertaService.calcular().stream().filter(a -> a.ativo() && a.dias() <= 30).count();
        return new ResumoPainel(total, abertas, por.get(StatusOS.FECHADA), por.get(StatusOS.AGUARDANDO_PECA),
                por.get(StatusOS.REABERTA), osDao.contarFechadasSemAssinaturaCliente(), proximas);
    }

    /** Últimos n meses (inclui o atual), com zeros nos meses sem OS. */
    public List<ResumoMensal> ultimosMeses(int n) {
        YearMonth atual = YearMonth.now();
        YearMonth inicio = atual.minusMonths(n - 1L);
        Map<YearMonth, ResumoMensal> existentes = new java.util.HashMap<>();
        for (ResumoMensal r : osDao.resumoPorMes(LocalDate.of(inicio.getYear(), inicio.getMonth(), 1))) {
            existentes.put(r.mes(), r);
        }
        List<ResumoMensal> lista = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            YearMonth m = inicio.plusMonths(i);
            lista.add(existentes.getOrDefault(m, new ResumoMensal(m, 0, 0)));
        }
        return lista;
    }

    public int pendentesAssinatura() {
        return osDao.contarFechadasSemAssinaturaCliente();
    }

    public Map<String, Integer> cargaPorTecnico() {
        return osDao.abertasPorTecnico(6);
    }

    public List<EventoHistorico> atividadeRecente(int n) {
        return historicoDao.recentes(n);
    }

    public List<OrdemServico> ultimasOS(int n) {
        return osDao.listarRecentes(n);
    }

    public List<AlertaPreventiva> proximasPreventivas(int n) {
        return alertaService.calcular().stream().filter(AlertaPreventiva::ativo).limit(n).toList();
    }
}
