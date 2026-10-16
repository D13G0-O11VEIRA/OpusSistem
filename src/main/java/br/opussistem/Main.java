package br.opussistem;

import javafx.application.Application;

/**
 * Ponto de entrada do programa. Esta classe NÃO estende Application de propósito:
 * assim o JavaFX também funciona quando executado pelo classpath (VS Code, jar único).
 */
public class Main {

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
