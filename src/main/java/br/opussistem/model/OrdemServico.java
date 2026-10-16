package br.opussistem.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ordem de serviço. Os campos descricaoProblema ("Natureza do serviço"),
 * laudoTecnico e observacoes guardam HTML do editor de texto formatado.
 */
public class OrdemServico {

    private Long id;
    private String numero;
    private String cliente;
    private String telefone;
    private String equipamento;
    private String modelo;
    private String fabricante;
    private String patrimonio;
    private String numeroSerie;
    private String statusEquipamento;
    private String tipoManutencao;
    private String tecnico;
    private BigDecimal valorPrevisto;
    private BigDecimal valorFinal;
    private LocalDateTime dataEntrada;
    private LocalDateTime previsaoEntrega;
    private LocalDateTime dataEntrega;
    private LocalDate proximaPreventiva;
    private StatusOS status = StatusOS.ABERTA;
    private String descricaoProblema;
    private String laudoTecnico;
    private String observacoes;
    private String rodapeRelatorio;
    private String assinaturaCliente;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEquipamento() { return equipamento; }
    public void setEquipamento(String equipamento) { this.equipamento = equipamento; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }
    public String getPatrimonio() { return patrimonio; }
    public void setPatrimonio(String patrimonio) { this.patrimonio = patrimonio; }
    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }
    public String getStatusEquipamento() { return statusEquipamento; }
    public void setStatusEquipamento(String statusEquipamento) { this.statusEquipamento = statusEquipamento; }
    public String getTipoManutencao() { return tipoManutencao; }
    public void setTipoManutencao(String tipoManutencao) { this.tipoManutencao = tipoManutencao; }
    public String getTecnico() { return tecnico; }
    public void setTecnico(String tecnico) { this.tecnico = tecnico; }
    public BigDecimal getValorPrevisto() { return valorPrevisto; }
    public void setValorPrevisto(BigDecimal valorPrevisto) { this.valorPrevisto = valorPrevisto; }
    public BigDecimal getValorFinal() { return valorFinal; }
    public void setValorFinal(BigDecimal valorFinal) { this.valorFinal = valorFinal; }
    public LocalDateTime getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDateTime dataEntrada) { this.dataEntrada = dataEntrada; }
    public LocalDateTime getPrevisaoEntrega() { return previsaoEntrega; }
    public void setPrevisaoEntrega(LocalDateTime previsaoEntrega) { this.previsaoEntrega = previsaoEntrega; }
    public LocalDateTime getDataEntrega() { return dataEntrega; }
    public void setDataEntrega(LocalDateTime dataEntrega) { this.dataEntrega = dataEntrega; }
    public LocalDate getProximaPreventiva() { return proximaPreventiva; }
    public void setProximaPreventiva(LocalDate proximaPreventiva) { this.proximaPreventiva = proximaPreventiva; }
    public StatusOS getStatus() { return status; }
    public void setStatus(StatusOS status) { this.status = status; }
    public String getDescricaoProblema() { return descricaoProblema; }
    public void setDescricaoProblema(String descricaoProblema) { this.descricaoProblema = descricaoProblema; }
    public String getLaudoTecnico() { return laudoTecnico; }
    public void setLaudoTecnico(String laudoTecnico) { this.laudoTecnico = laudoTecnico; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getRodapeRelatorio() { return rodapeRelatorio; }
    public void setRodapeRelatorio(String rodapeRelatorio) { this.rodapeRelatorio = rodapeRelatorio; }
    public String getAssinaturaCliente() { return assinaturaCliente; }
    public void setAssinaturaCliente(String assinaturaCliente) { this.assinaturaCliente = assinaturaCliente; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    /** Data usada para ordenar/comparar: fechamento, senão entrada. */
    public LocalDateTime dataReferencia() {
        return dataEntrega != null ? dataEntrega : dataEntrada;
    }

    @Override
    public String toString() {
        return numero + " — " + (equipamento == null ? "" : equipamento) + (cliente == null ? "" : " (" + cliente + ")");
    }
}
