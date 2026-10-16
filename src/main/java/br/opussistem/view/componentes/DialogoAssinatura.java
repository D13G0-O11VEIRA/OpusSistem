package br.opussistem.view.componentes;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Optional;

/** Janela para colher o nome e a assinatura desenhada. */
public final class DialogoAssinatura {

    public record Resultado(String nome, String email, byte[] png) {
    }

    private DialogoAssinatura() {
    }

    /**
     * @param assinaturaSalva PNG da assinatura salva do usuário (ou null) para reutilizar com um clique
     */
    public static Optional<Resultado> mostrar(String titulo, String nomePadrao, boolean pedirEmail, byte[] assinaturaSalva) {
        Dialog<Resultado> dialogo = new Dialog<>();
        dialogo.setTitle("Assinatura digital");
        dialogo.setHeaderText(titulo);
        dialogo.getDialogPane().getStylesheets().add(Ui.css());

        TextField nome = Ui.texto("Nome de quem assina");
        nome.setText(nomePadrao == null ? "" : nomePadrao);
        TextField email = Ui.texto("E-mail (opcional)");
        PainelAssinatura painel = new PainelAssinatura();

        VBox conteudo = new VBox(10, Ui.campo("Nome", nome));
        if (pedirEmail) {
            conteudo.getChildren().add(Ui.campo("E-mail", email));
        }
        conteudo.getChildren().add(Ui.campo("Assinatura", painel));
        javafx.scene.layout.HBox botoes = Ui.linha(Ui.secundario("Limpar", painel::limpar));
        if (assinaturaSalva != null) {
            botoes.getChildren().add(Ui.secundario("Usar minha assinatura salva", () -> painel.carregar(assinaturaSalva)));
        }
        conteudo.getChildren().add(botoes);
        dialogo.getDialogPane().setContent(conteudo);

        ButtonType assinar = new ButtonType("Assinar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(assinar, ButtonType.CANCEL);
        dialogo.getDialogPane().lookupButton(assinar).addEventFilter(ActionEvent.ACTION, ev -> {
            if (Ui.valor(nome) == null) {
                Dialogos.erro("Informe o nome de quem assina.");
                ev.consume();
            } else if (painel.vazio()) {
                Dialogos.erro("Desenhe a assinatura no quadro.");
                ev.consume();
            }
        });
        dialogo.setResultConverter(botao -> botao == assinar
                ? new Resultado(nome.getText().trim(), Ui.valor(email), painel.png()) : null);
        return dialogo.showAndWait();
    }
}
