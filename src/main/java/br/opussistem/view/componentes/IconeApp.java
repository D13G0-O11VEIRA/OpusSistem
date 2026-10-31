package br.opussistem.view.componentes;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Ícone e marca do OpusSistem (monograma). Aparecem só dentro do programa: janela, barra de tarefas, login e menu
 * lateral. Nunca saem nos relatórios, que usam apenas a logo da empresa do usuário.
 */
public final class IconeApp {

    private static final int[] TAMANHOS = { 16, 24, 32, 48, 64, 128, 256, 512 };
    private static List<Image> imagens;

    private IconeApp() {
    }

    /** Ícones da janela em vários tamanhos (o sistema escolhe o mais adequado). */
    public static synchronized List<Image> imagens() {
        if (imagens == null) {
            List<Image> lista = new ArrayList<>();
            for (int tamanho : TAMANHOS) {
                Image i = carregar("/icons/opussistem-" + tamanho + ".png");
                if (i != null) {
                    lista.add(i);
                }
            }
            imagens = lista;
        }
        return imagens;
    }

    /** Aplica o ícone do OpusSistem a uma janela (principal ou de diálogo). */
    public static void aplicar(Window janela) {
        if (janela instanceof Stage palco) {
            palco.getIcons().setAll(imagens());
        }
    }

    /** Ícone quadrado (fundo azul + monograma), por exemplo para a tela de login. */
    public static Image icone(int lado) {
        return carregar("/icons/opussistem-" + lado + ".png");
    }

    /** Monograma branco com fundo transparente (para fundos escuros, como o menu lateral). */
    public static Image monograma() {
        return carregar("/icons/monograma-branco.png");
    }

    private static Image carregar(String recurso) {
        try (InputStream in = IconeApp.class.getResourceAsStream(recurso)) {
            return in == null ? null : new Image(in);
        } catch (IOException e) {
            return null;
        }
    }
}
