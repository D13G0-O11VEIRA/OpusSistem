package br.opussistem.model;

import java.time.LocalDateTime;

public class Usuario {

    private Long id;
    private String nome;
    private String usuario;
    private String email;
    private String cargo;
    private PerfilUsuario perfil = PerfilUsuario.OPERADOR;
    private String senhaHash;
    private String assinaturaArquivo;
    private boolean ativo = true;
    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public PerfilUsuario getPerfil() { return perfil; }
    public void setPerfil(PerfilUsuario perfil) { this.perfil = perfil; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public String getAssinaturaArquivo() { return assinaturaArquivo; }
    public void setAssinaturaArquivo(String assinaturaArquivo) { this.assinaturaArquivo = assinaturaArquivo; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public boolean isAdmin() {
        return perfil == PerfilUsuario.ADMIN;
    }

    @Override
    public String toString() {
        return nome;
    }
}
