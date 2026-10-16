package br.opussistem.service;

import br.opussistem.model.AlertaPreventiva;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.Prioridade;
import br.opussistem.model.StatusOS;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertaServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 13);

    private static OrdemServico os(long id, String pat, String serie, StatusOS status, LocalDate entrada, LocalDate proxima) {
        OrdemServico o = new OrdemServico();
        o.setId(id);
        o.setPatrimonio(pat);
        o.setNumeroSerie(serie);
        o.setStatus(status);
        o.setDataEntrada(entrada.atStartOfDay());
        if (status == StatusOS.FECHADA) {
            o.setDataEntrega(entrada.plusDays(2).atStartOfDay());
        }
        o.setProximaPreventiva(proxima);
        return o;
    }

    @Test
    void prioridadePorFaixaDeDias() {
        assertEquals(Prioridade.VENCIDA, AlertaService.prioridade(-1, true, true, true));
        assertEquals(Prioridade.URGENTE, AlertaService.prioridade(0, true, true, true));
        assertEquals(Prioridade.URGENTE, AlertaService.prioridade(30, true, true, true));
        assertEquals(Prioridade.PROGRAMAR, AlertaService.prioridade(31, true, true, true));
        assertEquals(Prioridade.PLANEJAR, AlertaService.prioridade(61, true, true, true));
        assertEquals(Prioridade.EM_DIA, AlertaService.prioridade(91, true, true, true));
    }

    @Test
    void regraDesligadaTornaAFaixaEmDia() {
        assertEquals(Prioridade.EM_DIA, AlertaService.prioridade(20, false, true, true));
        assertEquals(Prioridade.EM_DIA, AlertaService.prioridade(45, true, false, true));
        assertEquals(Prioridade.EM_DIA, AlertaService.prioridade(80, true, true, false));
        assertEquals(Prioridade.VENCIDA, AlertaService.prioridade(-5, false, false, false));
    }

    @Test
    void usaOsMaisRecenteDoEquipamentoEMarcaProgramada() {
        List<OrdemServico> todas = new ArrayList<>();
        todas.add(os(1, "108", "A1", StatusOS.FECHADA, HOJE.minusDays(200), HOJE.minusDays(20)));
        todas.add(os(2, "108", "A1", StatusOS.ABERTA, HOJE.minusDays(1), HOJE.plusDays(170)));
        todas.add(os(3, "205", "B2", StatusOS.FECHADA, HOJE.minusDays(100), null));

        List<AlertaPreventiva> alertas = AlertaService.calcular(todas, HOJE, true, true, true, 365);

        assertEquals(2, alertas.size());
        AlertaPreventiva primeiro = alertas.get(0); // menor "dias" primeiro
        assertEquals(2L, primeiro.os().getId().longValue()); // a OS mais recente do equipamento 108
        assertEquals(170, primeiro.dias());
        assertTrue(primeiro.programada());
        assertEquals(Prioridade.EM_DIA, primeiro.prioridade());

        AlertaPreventiva segundo = alertas.get(1);
        assertEquals(3L, segundo.os().getId().longValue());
        // sem próxima preventiva: última manutenção concluída + 365 dias
        assertEquals(HOJE.minusDays(100).plusDays(2).plusDays(365), segundo.vencimento());
        assertFalse(segundo.programada());
    }

    @Test
    void equipamentoSemPatrimonioNemSerieNaoSeMistura() {
        List<OrdemServico> todas = List.of(
                os(1, null, null, StatusOS.FECHADA, HOJE.minusDays(10), HOJE.plusDays(5)),
                os(2, null, null, StatusOS.FECHADA, HOJE.minusDays(10), HOJE.plusDays(50)));
        assertEquals(2, AlertaService.calcular(todas, HOJE, true, true, true, 365).size());
    }
}
