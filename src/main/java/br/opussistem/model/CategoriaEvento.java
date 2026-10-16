package br.opussistem.model;

/** Tipo de evento da linha do tempo (define a cor do marcador). */
public enum CategoriaEvento {
    ABERTURA("#2f6fe4"),
    ALTERACAO("#8250df"),
    ANDAMENTO("#f08c1a"),
    AGUARDANDO("#e0a800"),
    FECHAMENTO("#1f9d57"),
    REABERTURA("#d93b3b"),
    ASSINATURA("#1f9d57"),
    IMPORTACAO("#8250df");

    private final String cor;

    CategoriaEvento(String cor) {
        this.cor = cor;
    }

    public String getCor() {
        return cor;
    }
}
