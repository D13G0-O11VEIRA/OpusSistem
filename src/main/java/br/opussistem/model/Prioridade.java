package br.opussistem.model;

/** Prioridade de um alerta de preventiva. */
public enum Prioridade {
    VENCIDA("Vencida", "badge-vermelho"),
    URGENTE("Urgente", "badge-vermelho"),
    PROGRAMAR("Programar", "badge-laranja"),
    PLANEJAR("Planejar", "badge-amarelo"),
    EM_DIA("Em dia", "badge-verde");

    private final String rotulo;
    private final String classeCss;

    Prioridade(String rotulo, String classeCss) {
        this.rotulo = rotulo;
        this.classeCss = classeCss;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getClasseCss() {
        return classeCss;
    }
}
