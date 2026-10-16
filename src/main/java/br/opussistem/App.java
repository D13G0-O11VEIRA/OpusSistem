package br.opussistem;

import br.opussistem.controller.Contexto;
import br.opussistem.model.Usuario;
import br.opussistem.util.MigradorBanco;
import br.opussistem.view.LoginView;
import br.opussistem.view.MainView;
import br.opussistem.view.componentes.Fontes;
import br.opussistem.view.componentes.Tema;
import br.opussistem.view.componentes.Ui;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;

/** Aplicação JavaFX: prepara o banco, mostra o login e depois a janela principal. */
public class App extends Application {

    private static final System.Logger LOG = System.getLogger(App.class.getName());

    private static HostServices servicos;
    private static Scene cena;

    @Override
    public void start(Stage palco) {
        servicos = getHostServices();
        Fontes.carregar();
        palco.setTitle("OpusSistem");
        palco.setMinWidth(1100);
        palco.setMinHeight(700);
        cena = new Scene(new StackPane(), 1320, 820);
        cena.getStylesheets().add(Ui.css());
        palco.setScene(cena);
        iniciar();
        palco.show();
    }

    private static void iniciar() {
        try {
            MigradorBanco.migrar();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR, "Falha ao iniciar o banco", e);
            mostrarErroInicial(e.getMessage() == null ? e.toString() : e.getMessage());
            return;
        }
        mostrarLogin();
    }

    private static void mostrarLogin() {
        definirRaiz(new LoginView(App::aoEntrar));
    }

    private static void aoEntrar(Usuario usuario) {
        Contexto.entrar(usuario);
        definirRaiz(new MainView(App::aoSair));
    }

    private static void aoSair() {
        Contexto.sair();
        mostrarLogin();
    }

    private static void definirRaiz(Parent raiz) {
        cena.setRoot(raiz);
        reaplicarTema();
    }

    /** Relê cores e tema (claro/escuro) das configurações e aplica na janela. */
    public static void reaplicarTema() {
        try {
            Tema.aplicar(cena.getRoot());
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Não foi possível aplicar o tema", e);
        }
    }

    /** Abre um arquivo (PDF, por exemplo) no programa padrão do sistema. */
    public static void abrir(Path arquivo) {
        servicos.showDocument(arquivo.toAbsolutePath().toUri().toString());
    }

    private static void mostrarErroInicial(String mensagem) {
        Label titulo = new Label("Não foi possível iniciar o OpusSistem");
        titulo.getStyleClass().add("titulo-pagina");
        Label detalhe = new Label(mensagem);
        detalhe.setWrapText(true);
        Button tentar = Ui.botao("Tentar novamente", App::iniciar);
        Button fechar = Ui.secundario("Fechar", Platform::exit);
        VBox caixa = new VBox(14, titulo, detalhe, Ui.linha(tentar, fechar));
        caixa.getStyleClass().add("card");
        caixa.setMaxWidth(560);
        caixa.setPadding(new Insets(28));
        StackPane fundo = new StackPane(caixa);
        fundo.getStyleClass().add("fundo-login");
        StackPane.setAlignment(caixa, Pos.CENTER);
        cena.setRoot(fundo);
    }
}
