package br.opussistem.view.componentes;

import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;

/**
 * Carrega a fonte Inter (a mesma da versão web) se os arquivos estiverem em src/main/resources/fonts.
 * Sem eles, o CSS usa "Segoe UI" e outras fontes do sistema. Veja src/main/resources/fonts/LEIA-ME.md.
 */
public final class Fontes {

    private static final System.Logger LOG = System.getLogger(Fontes.class.getName());
    private static final String[] ARQUIVOS = { "Inter-Regular.ttf", "Inter-Medium.ttf", "Inter-SemiBold.ttf", "Inter-Bold.ttf" };

    private Fontes() {
    }

    public static void carregar() {
        for (String arquivo : ARQUIVOS) {
            try (InputStream in = Fontes.class.getResourceAsStream("/fonts/" + arquivo)) {
                if (in != null) {
                    Font.loadFont(in, 14);
                }
            } catch (IOException e) {
                LOG.log(System.Logger.Level.WARNING, "Não foi possível carregar a fonte " + arquivo, e);
            }
        }
    }
}
