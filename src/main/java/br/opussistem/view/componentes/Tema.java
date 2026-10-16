package br.opussistem.view.componentes;

import br.opussistem.controller.Contexto;
import br.opussistem.service.ConfiguracaoService;
import javafx.scene.Parent;

import java.util.regex.Pattern;

/** Aplica cores de destaque/menu e tema claro ou escuro à janela. */
public final class Tema {

    private static final Pattern COR = Pattern.compile("#[0-9a-fA-F]{6}");

    private Tema() {
    }

    public static void aplicar(Parent raiz) {
        ConfiguracaoService c = Contexto.CONFIG;
        String destaque = cor(c.get(ConfiguracaoService.COR_DESTAQUE), "#2f6fe4");
        String menu = cor(c.get(ConfiguracaoService.COR_MENU), "#0d2a63");
        raiz.setStyle("-cor-destaque: " + destaque + "; -cor-menu: " + menu + ";");
        raiz.getStyleClass().remove("escuro");
        if ("ESCURO".equalsIgnoreCase(c.get(ConfiguracaoService.TEMA))) {
            raiz.getStyleClass().add("escuro");
        }
    }

    private static String cor(String valor, String padrao) {
        return valor != null && COR.matcher(valor).matches() ? valor : padrao;
    }
}
