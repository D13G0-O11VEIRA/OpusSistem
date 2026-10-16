package br.opussistem.model;

public enum PerfilUsuario {
    ADMIN("Administrador"),
    OPERADOR("Operador");

    private final String rotulo;

    PerfilUsuario(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
