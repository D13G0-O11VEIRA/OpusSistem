package br.opussistem.model;

import java.time.LocalDateTime;

/** Assinatura digital (imagem desenhada) de uma OS. */
public class Assinatura {

    private Long id;
    private long osId;
    private PapelAssinatura papel;
    private String nome;
    private String email;
    private String arquivo;
    private String codigoVerificacao;
    private String pacoteCodigo;
    private LocalDateTime assinadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public long getOsId() { return osId; }
    public void setOsId(long osId) { this.osId = osId; }
    public PapelAssinatura getPapel() { return papel; }
    public void setPapel(PapelAssinatura papel) { this.papel = papel; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getArquivo() { return arquivo; }
    public void setArquivo(String arquivo) { this.arquivo = arquivo; }
    public String getCodigoVerificacao() { return codigoVerificacao; }
    public void setCodigoVerificacao(String codigoVerificacao) { this.codigoVerificacao = codigoVerificacao; }
    public String getPacoteCodigo() { return pacoteCodigo; }
    public void setPacoteCodigo(String pacoteCodigo) { this.pacoteCodigo = pacoteCodigo; }
    public LocalDateTime getAssinadoEm() { return assinadoEm; }
    public void setAssinadoEm(LocalDateTime assinadoEm) { this.assinadoEm = assinadoEm; }
}
