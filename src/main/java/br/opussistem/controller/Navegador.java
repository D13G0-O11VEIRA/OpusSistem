package br.opussistem.controller;

import java.util.function.BiConsumer;

/**
 * Navegação entre telas. A janela principal registra o destino; as telas só pedem
 * "ir para a rota X" (opcionalmente com um parâmetro, como o id de uma OS).
 */
public final class Navegador {

    private static BiConsumer<Rota, Object> destino;

    private Navegador() {
    }

    public static void registrar(BiConsumer<Rota, Object> novoDestino) {
        destino = novoDestino;
    }

    public static void ir(Rota rota) {
        ir(rota, null);
    }

    public static void ir(Rota rota, Object parametro) {
        if (destino != null) {
            destino.accept(rota, parametro);
        }
    }
}
