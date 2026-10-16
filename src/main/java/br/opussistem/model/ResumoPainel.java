package br.opussistem.model;

/** Números do dashboard. */
public record ResumoPainel(int total, int abertas, int fechadas, int aguardandoPeca, int reabertas,
                           int pendentesAssinatura, int preventivasProximas) {
}
