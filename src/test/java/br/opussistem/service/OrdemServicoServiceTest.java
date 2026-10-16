package br.opussistem.service;

import br.opussistem.exception.AppException;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.StatusOS;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrdemServicoServiceTest {

    private static OrdemServico valida() {
        OrdemServico os = new OrdemServico();
        os.setCliente("Hospital Exemplo");
        os.setEquipamento("Monitor");
        os.setDataEntrada(LocalDateTime.now());
        return os;
    }

    @Test
    void exigeClienteEquipamentoEDataDeEntrada() {
        OrdemServico semCliente = valida();
        semCliente.setCliente(" ");
        assertThrows(AppException.class, () -> OrdemServicoService.validar(semCliente));

        OrdemServico semEquipamento = valida();
        semEquipamento.setEquipamento(null);
        assertThrows(AppException.class, () -> OrdemServicoService.validar(semEquipamento));

        OrdemServico semData = valida();
        semData.setDataEntrada(null);
        assertThrows(AppException.class, () -> OrdemServicoService.validar(semData));
    }

    @Test
    void rejeitaValorNegativoETextoLongoDemais() {
        OrdemServico negativo = valida();
        negativo.setValorFinal(new BigDecimal("-1"));
        assertThrows(AppException.class, () -> OrdemServicoService.validar(negativo));

        OrdemServico longo = valida();
        longo.setTelefone("1".repeat(31));
        assertThrows(AppException.class, () -> OrdemServicoService.validar(longo));
    }

    @Test
    void aceitaOsValida() {
        OrdemServicoService.validar(valida());
    }

    @Test
    void statusAceitaTextosDeOutrosSistemas() {
        assertEquals(StatusOS.FECHADA, StatusOS.deTexto("Concluída"));
        assertEquals(StatusOS.AGUARDANDO_PECA, StatusOS.deTexto("Aguardando peça"));
        assertEquals(StatusOS.EM_ANDAMENTO, StatusOS.deTexto("Em execução"));
        assertEquals(StatusOS.REABERTA, StatusOS.deTexto("Reaberta"));
        assertEquals(StatusOS.ABERTA, StatusOS.deTexto(""));
        assertEquals(StatusOS.FECHADA, StatusOS.deTexto("FECHADA"));
    }
}
