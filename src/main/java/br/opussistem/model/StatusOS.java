package br.opussistem.model;

import java.text.Normalizer;

/** Situação de uma ordem de serviço (gravada no banco pelo nome do enum). */
public enum StatusOS {
    ABERTA("Aberta"),
    EM_ANDAMENTO("Em andamento"),
    AGUARDANDO_PECA("Aguardando peça"),
    REABERTA("Reaberta"),
    FECHADA("Fechada");

    private final String rotulo;

    StatusOS(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean estaAberta() {
        return this != FECHADA;
    }

    @Override
    public String toString() {
        return rotulo;
    }

    /** Aceita o nome do enum, o rótulo ou textos parecidos vindos de outros sistemas. */
    public static StatusOS deTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return ABERTA;
        }
        String t = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
        if (t.contains("reabert")) {
            return REABERTA;
        }
        if (t.matches(".*(fech|conclu|finaliz|entreg|encerr|pago|faturad).*")) {
            return FECHADA;
        }
        if (t.matches(".*(aguard|peca|espera).*")) {
            return AGUARDANDO_PECA;
        }
        if (t.matches(".*(andam|execu|manut|process|servic).*")) {
            return EM_ANDAMENTO;
        }
        return ABERTA;
    }
}
