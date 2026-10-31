package br.opussistem.controller;

/**
 * Aviso de que a identidade visual da empresa (logo e nome fantasia) mudou. A janela principal registra um ouvinte
 * e redesenha o topo do menu lateral; o Perfil chama {@link #atualizar()} depois de salvar.
 */
public final class Marca {

    private static Runnable ouvinte;

    private Marca() {
    }

    public static void registrar(Runnable novoOuvinte) {
        ouvinte = novoOuvinte;
    }

    public static void atualizar() {
        if (ouvinte != null) {
            ouvinte.run();
        }
    }
}
