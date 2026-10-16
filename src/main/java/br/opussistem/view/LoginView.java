package br.opussistem.view;

import br.opussistem.controller.Contexto;
import br.opussistem.model.Usuario;
import br.opussistem.service.ConfiguracaoService;
import br.opussistem.util.Armazenamento;
import br.opussistem.view.componentes.Toast;
import br.opussistem.view.componentes.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Tela inicial: "Entrar" e "Criar conta" lado a lado em abas (igual à versão web). */
public class LoginView extends StackPane {

    private final Consumer<Usuario> aoEntrar;
    private final VBox cartao = new VBox(12);
    private boolean modoCadastro;

    public LoginView(Consumer<Usuario> aoEntrar) {
        this.aoEntrar = aoEntrar;
        getStyleClass().add("fundo-login");
        cartao.getStyleClass().add("card");
        cartao.setPadding(new Insets(24));
        cartao.setPrefWidth(400);
        cartao.setMinWidth(400);
        cartao.setMaxWidth(400);
        cartao.setMaxHeight(USE_PREF_SIZE);
        getChildren().add(cartao);
        StackPane.setAlignment(cartao, Pos.CENTER);
        Toast.registrar(this);
        modoCadastro = !Contexto.AUTH.existemUsuarios();
        montar();
    }

    private void montar() {
        cartao.getChildren().clear();
        cartao.setAlignment(Pos.TOP_CENTER);

        Path logo = Armazenamento.logo();
        if (Files.exists(logo)) {
            cartao.getChildren().add(new ImageView(new Image(logo.toUri().toString(), 200, 64, true, true)));
        }
        Label opus = new Label("Opus");
        opus.getStyleClass().add("marca-login-opus");
        Label sistem = new Label("Sistem");
        sistem.getStyleClass().add("marca-login-sistem");
        HBox marca = new HBox(0, opus, sistem);
        marca.setAlignment(Pos.CENTER);
        String fantasia = Contexto.CONFIG.get(ConfiguracaoService.EMPRESA_FANTASIA);
        Label sub = new Label(fantasia.isBlank() ? "Gestão de ordens de serviço" : fantasia);
        sub.getStyleClass().add("subtitulo");
        VBox titulos = new VBox(0, marca, sub);
        titulos.setAlignment(Pos.CENTER);

        ToggleGroup grupo = new ToggleGroup();
        ToggleButton entrar = aba("Entrar", grupo, !modoCadastro, false);
        ToggleButton criar = aba("Criar conta", grupo, modoCadastro, true);
        HBox abas = new HBox(4, entrar, criar);
        abas.getStyleClass().add("trilho-abas");

        cartao.getChildren().addAll(titulos, abas, modoCadastro ? formularioCadastro() : formularioEntrada());
        Label rodape = new Label("Os dados ficam salvos no banco MySQL configurado.");
        rodape.getStyleClass().add("dica-texto");
        cartao.getChildren().add(rodape);
    }

    private ToggleButton aba(String texto, ToggleGroup grupo, boolean selecionada, boolean paraCadastro) {
        ToggleButton b = new ToggleButton(texto);
        b.setToggleGroup(grupo);
        b.getStyleClass().add("aba");
        b.setSelected(selecionada);
        b.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setOnAction(e -> {
            modoCadastro = paraCadastro;
            montar();
        });
        return b;
    }

    private VBox formularioEntrada() {
        TextField usuario = Ui.texto("seu usuário");
        PasswordField senha = new PasswordField();
        senha.setPromptText("sua senha");
        Button entrar = Ui.botao("Entrar", () -> {
            Usuario u = Contexto.AUTH.entrar(usuario.getText(), senha.getText());
            aoEntrar.accept(u);
        });
        entrar.setDefaultButton(true);
        entrar.setMaxWidth(Double.MAX_VALUE);
        VBox form = new VBox(10, Ui.campo("Usuário", usuario), Ui.campo("Senha", senha), entrar);
        if (!Contexto.AUTH.existemUsuarios()) {
            Label aviso = new Label("Ainda não há usuários. Use a aba \"Criar conta\".");
            aviso.getStyleClass().add("dica-texto");
            form.getChildren().add(aviso);
        }
        return form;
    }

    private VBox formularioCadastro() {
        TextField nome = Ui.texto("nome completo");
        TextField usuario = Ui.texto("ex.: maria.silva");
        TextField email = Ui.texto("opcional");
        PasswordField senha = new PasswordField();
        senha.setPromptText("mínimo de 6 caracteres");
        PasswordField confirmacao = new PasswordField();
        confirmacao.setPromptText("repita a senha");
        Button criar = Ui.botao("Criar conta e entrar", () -> {
            Usuario u = Contexto.AUTH.cadastrar(nome.getText(), usuario.getText(), email.getText(),
                    senha.getText(), confirmacao.getText(), null);
            aoEntrar.accept(u);
        });
        criar.setDefaultButton(true);
        criar.setMaxWidth(Double.MAX_VALUE);
        VBox form = new VBox(10, Ui.campo("Nome completo", nome), Ui.campo("Usuário de acesso", usuario),
                Ui.campo("E-mail (opcional)", email), Ui.campo("Senha", senha), Ui.campo("Confirmar senha", confirmacao), criar);
        if (!Contexto.AUTH.existemUsuarios()) {
            Label aviso = new Label("O primeiro cadastro será o administrador do sistema.");
            aviso.getStyleClass().add("dica-texto");
            form.getChildren().add(aviso);
        }
        return form;
    }
}
