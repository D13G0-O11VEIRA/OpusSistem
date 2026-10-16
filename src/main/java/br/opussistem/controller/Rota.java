package br.opussistem.controller;

/** Telas do sistema. As que têm ícone aparecem no menu lateral. */
public enum Rota {
    DASHBOARD("Dashboard", "dash"),
    CADASTRO("Cadastro", "cad"),
    ALTERAR("Alterar OS", "alt"),
    LOCALIZAR("Localizar OS", "loc"),
    REABRIR("Reabrir OS", "reab"),
    RELATORIOS("Relatórios", "rel"),
    LOTE("Assinatura em lote", "lote"),
    HISTORICO("Históricos", "hist"),
    ALERTAS("Alertas de preventivas", "alert"),
    IMPORTAR_EXPORTAR("Exportar / Importar", "io"),
    PERFIL("Perfil e configurações", null);

    private final String titulo;
    private final String icone;

    Rota(String titulo, String icone) {
        this.titulo = titulo;
        this.icone = icone;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getIcone() {
        return icone;
    }

    public boolean noMenu() {
        return icone != null;
    }
}
