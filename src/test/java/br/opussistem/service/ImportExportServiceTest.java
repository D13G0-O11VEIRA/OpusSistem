package br.opussistem.service;

import br.opussistem.model.OrdemServico;
import br.opussistem.model.StatusOS;
import br.opussistem.service.ImportExportService.Campo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImportExportServiceTest {

    private final ImportExportService servico = new ImportExportService(new OrdemServicoService());

    @Test
    void reconheceColunasDeUmSistemaDeOficina() {
        List<String> colunas = List.of("Número da OS", "Data de entrada", "Cliente", "Placa", "Veículo", "Marca", "Chassi",
                "KM", "Mecânico", "Serviços solicitados", "Diagnóstico", "Situação", "Valor total", "Data de saída");
        Map<Campo, String> m = servico.sugerirMapeamento(colunas);
        assertEquals("Número da OS", m.get(Campo.NUMERO));
        assertEquals("Data de entrada", m.get(Campo.ENTRADA));
        assertEquals("Cliente", m.get(Campo.CLIENTE));
        assertEquals("Placa", m.get(Campo.PATRIMONIO));
        assertEquals("Veículo", m.get(Campo.EQUIPAMENTO));
        assertEquals("Marca", m.get(Campo.FABRICANTE));
        assertEquals("Chassi", m.get(Campo.SERIE));
        assertEquals("Mecânico", m.get(Campo.TECNICO));
        assertEquals("Serviços solicitados", m.get(Campo.NATUREZA));
        assertEquals("Diagnóstico", m.get(Campo.LAUDO));
        assertEquals("Situação", m.get(Campo.STATUS));
        assertEquals("Valor total", m.get(Campo.VALOR));
        assertEquals("Data de saída", m.get(Campo.SAIDA));
        assertNull(m.get(Campo.TELEFONE));
    }

    @Test
    void statusDoEquipamentoNaoVirraStatusDaOs() {
        Map<Campo, String> m = servico.sugerirMapeamento(List.of("Status do equipamento", "Status da OS"));
        assertEquals("Status do equipamento", m.get(Campo.STATUS_EQUIPAMENTO));
        assertEquals("Status da OS", m.get(Campo.STATUS));
    }

    @Test
    void reconheceOsCabecalhosDoProprioCsvExportado() {
        List<String> cab = java.util.Arrays.stream(Campo.values()).map(Campo::rotulo).toList();
        Map<Campo, String> m = servico.sugerirMapeamento(cab);
        for (Campo c : Campo.values()) {
            assertEquals(c.rotulo(), m.get(c), "campo " + c);
        }
    }

    @Test
    void converteLinhaComDatasValorEStatus() {
        Map<Campo, String> mapa = Map.of(Campo.CLIENTE, "Cliente", Campo.ENTRADA, "Entrada", Campo.SAIDA, "Saída",
                Campo.VALOR, "Valor", Campo.STATUS, "Situação", Campo.NATUREZA, "Serviço");
        Map<String, String> linha = new LinkedHashMap<>();
        linha.put("Cliente", "Maria");
        linha.put("Entrada", "05/10/2026");
        linha.put("Saída", "07/10/2026");
        linha.put("Valor", "1.234,56");
        linha.put("Situação", "Concluída");
        linha.put("Serviço", "Troca de pastilhas");

        OrdemServico os = servico.converterLinha(linha, mapa, StatusOS.ABERTA);

        assertEquals("Maria", os.getCliente());
        assertEquals("(não informado)", os.getEquipamento());
        assertEquals(LocalDate.of(2026, 10, 5), os.getDataEntrada().toLocalDate());
        assertEquals(LocalDate.of(2026, 10, 7), os.getDataEntrega().toLocalDate());
        assertEquals(new BigDecimal("1234.56"), os.getValorFinal());
        assertEquals(StatusOS.FECHADA, os.getStatus());
        assertEquals("<p>Troca de pastilhas</p>", os.getDescricaoProblema());
    }

    @Test
    void semStatusMapeadoUsaOPadraoOuFechadaSeHouverSaida() {
        Map<Campo, String> mapa = Map.of(Campo.CLIENTE, "Cliente", Campo.SAIDA, "Saída");
        Map<String, String> comSaida = Map.of("Cliente", "A", "Saída", "10/10/2026");
        Map<String, String> semSaida = Map.of("Cliente", "B", "Saída", "");
        assertEquals(StatusOS.FECHADA, servico.converterLinha(comSaida, mapa, StatusOS.ABERTA).getStatus());
        assertEquals(StatusOS.EM_ANDAMENTO, servico.converterLinha(semSaida, mapa, StatusOS.EM_ANDAMENTO).getStatus());
    }
}
