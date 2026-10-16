package br.opussistem.model;

import java.time.LocalDate;

/** Critérios de pesquisa de OS (campos nulos ou vazios são ignorados). */
public class FiltroOS {

    public String numero;
    public String cliente;
    public String patrimonio;
    public String serie;
    public String fabricante;
    public String tecnico;
    public StatusOS status;
    public LocalDate dataInicial;
    public LocalDate dataFinal;
    /** Se true, só OS fechadas (usado por Reabrir e Assinatura em lote). */
    public boolean somenteFechadas;

    public FiltroOS numero(String v) { this.numero = v; return this; }
    public FiltroOS cliente(String v) { this.cliente = v; return this; }
    public FiltroOS patrimonio(String v) { this.patrimonio = v; return this; }
    public FiltroOS serie(String v) { this.serie = v; return this; }
    public FiltroOS fabricante(String v) { this.fabricante = v; return this; }
    public FiltroOS tecnico(String v) { this.tecnico = v; return this; }
    public FiltroOS status(StatusOS v) { this.status = v; return this; }
    public FiltroOS periodo(LocalDate ini, LocalDate fim) { this.dataInicial = ini; this.dataFinal = fim; return this; }
    public FiltroOS somenteFechadas(boolean v) { this.somenteFechadas = v; return this; }
}
