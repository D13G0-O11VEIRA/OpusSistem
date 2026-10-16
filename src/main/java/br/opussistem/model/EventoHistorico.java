package br.opussistem.model;

import java.time.LocalDateTime;

public class EventoHistorico {

    private Long id;
    private long osId;
    private String numeroOs; // preenchido nas consultas com JOIN
    private LocalDateTime dataHora;
    private CategoriaEvento categoria;
    private String titulo;
    private String descricao;
    private String responsavel;

    public EventoHistorico() {
    }

    public EventoHistorico(long osId, CategoriaEvento categoria, String titulo, String descricao, String responsavel) {
        this.osId = osId;
        this.dataHora = LocalDateTime.now();
        this.categoria = categoria;
        this.titulo = titulo;
        this.descricao = descricao;
        this.responsavel = responsavel;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getOsId() { return osId; }
    public void setOsId(long osId) { this.osId = osId; }
    public String getNumeroOs() { return numeroOs; }
    public void setNumeroOs(String numeroOs) { this.numeroOs = numeroOs; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public CategoriaEvento getCategoria() { return categoria; }
    public void setCategoria(CategoriaEvento categoria) { this.categoria = categoria; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
}
