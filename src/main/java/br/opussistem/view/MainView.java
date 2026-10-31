package br.opussistem.view;

import br.opussistem.controller.Contexto;
import br.opussistem.controller.Marca;
import br.opussistem.controller.Navegador;
import br.opussistem.controller.Rota;
import br.opussistem.model.OrdemServico;
import br.opussistem.service.ConfiguracaoService;
import br.opussistem.view.componentes.IconeApp;
import br.opussistem.view.componentes.Icones;
import br.opussistem.view.componentes.Toast;
import br.opussistem.view.componentes.Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.util.EnumMap;
import java.util.Map;

/** Janela principal: menu lateral azul, barra superior (o nome do usuário abre o perfil) e a área da tela. */
public class MainView extends StackPane {

    private static final System.Logger LOG = System.getLogger(MainView.class.getName());

    private final StackPane conteudo = new StackPane();
    private final ToggleGroup grupo = new ToggleGroup();
    private final Map<Rota, ToggleButton> botoes = new EnumMap<>(Rota.class);
    private final Map<Rota, StackPane> selos = new EnumMap<>(Rota.class);
    private final Button botaoUsuario = new Button();
    private final Label tituloApp = new Label();
    private final VBox topoMarca = new VBox(8);

    public MainView(Runnable aoSair) {
        BorderPane layout = new BorderPane();
        layout.setLeft(menuLateral());
        layout.setTop(barraTopo(aoSair));
        layout.setCenter(conteudo);
        getChildren().add(layout);
        Toast.registrar(this);
        Navegador.registrar(this::mostrar);
        Marca.registrar(this::montarMarca);
        mostrar(Rota.DASHBOARD, null);
    }

    // ------------------------------------------------------------ estrutura

    private VBox menuLateral() {
        VBox menu = new VBox(4);
        menu.getStyleClass().add("menu-lateral");
        menu.setPrefWidth(222);
        menu.setMinWidth(222);
        menu.setMaxWidth(222);

        topoMarca.setPadding(new Insets(0, 0, 18, 8));
        montarMarca();
        menu.getChildren().add(topoMarca);

        for (Rota rota : Rota.values()) {
            if (rota.noMenu()) {
                menu.getChildren().add(itemDoMenu(rota));
            }
        }
        return menu;
    }

    /**
     * Topo do menu: marca do OpusSistem (sempre) e, abaixo, a logo e o nome fantasia da empresa do usuário. A logo da
     * empresa só aparece depois de enviada em Perfil > Empresa e relatório, com o tamanho escolhido lá.
     */
    private void montarMarca() {
        topoMarca.getChildren().clear();

        HBox marca = new HBox(8);
        marca.setAlignment(Pos.CENTER_LEFT);
        Image monograma = IconeApp.monograma();
        if (monograma != null) {
            ImageView vista = new ImageView(monograma);
            vista.setFitHeight(30);
            vista.setPreserveRatio(true);
            vista.setSmooth(true);
            marca.getChildren().add(vista);
        }
        Label opus = new Label("Opus");
        opus.getStyleClass().add("marca-opus");
        Label sistem = new Label("Sistem");
        sistem.getStyleClass().add("marca-sistem");
        marca.getChildren().add(new HBox(0, opus, sistem));
        topoMarca.getChildren().add(marca);

        Contexto.LOGO.imagem().ifPresent(bytes -> {
            int lado = Contexto.LOGO.tamanhoMenu();
            ImageView logoEmpresa = new ImageView(new Image(new ByteArrayInputStream(bytes)));
            logoEmpresa.setPreserveRatio(true);
            logoEmpresa.setSmooth(true);
            logoEmpresa.setFitWidth(lado);
            logoEmpresa.setFitHeight(lado);
            VBox.setMargin(logoEmpresa, new Insets(12, 0, 0, 0));
            topoMarca.getChildren().add(logoEmpresa);
        });
        String fantasia = Contexto.CONFIG.get(ConfiguracaoService.EMPRESA_FANTASIA);
        if (!fantasia.isBlank()) {
            Label nome = new Label(fantasia);
            nome.getStyleClass().add("marca-empresa");
            topoMarca.getChildren().add(nome);
        }
    }

    private ToggleButton itemDoMenu(Rota rota) {
        ToggleButton botao = new ToggleButton();
        botao.setToggleGroup(grupo);
        botao.getStyleClass().add("menu-item");
        botao.setMaxWidth(Double.MAX_VALUE);
        botao.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        Label texto = new Label(rota.getTitulo());
        Label numero = new Label();
        StackPane selo = new StackPane(numero);
        selo.getStyleClass().add("selo");
        selo.setVisible(false);
        selo.setManaged(false);
        HBox linha = new HBox(10, Icones.criar(rota.getIcone(), 18), texto, Ui.espaco(), selo);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.setPrefWidth(190);
        botao.setGraphic(linha);

        botao.setOnAction(e -> {
            botao.setSelected(true);
            Navegador.ir(rota);
        });
        botoes.put(rota, botao);
        selos.put(rota, selo);
        return botao;
    }

    private HBox barraTopo(Runnable aoSair) {
        tituloApp.getStyleClass().add("titulo-app");
        botaoUsuario.getStyleClass().add("usuario");
        botaoUsuario.setOnAction(e -> Navegador.ir(Rota.PERFIL));
        botaoUsuario.setTooltip(new Tooltip("Abrir perfil e configurações"));
        Button sair = new Button("Sair");
        sair.getStyleClass().add("usuario");
        sair.setOnAction(e -> aoSair.run());
        HBox barra = new HBox(8, tituloApp, Ui.espaco(), botaoUsuario, sair);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("barra-topo");
        return barra;
    }

    private void atualizarUsuario() {
        String nome = Contexto.usuario().getNome();
        HBox grafico = new HBox(8, Ui.avatar(nome), new Label(nome));
        grafico.setAlignment(Pos.CENTER_LEFT);
        botaoUsuario.setGraphic(grafico);
        botaoUsuario.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    // ------------------------------------------------------------ navegação

    private void mostrar(Rota rota, Object parametro) {
        Node tela;
        try {
            tela = criarTela(rota, parametro);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR, "Erro ao abrir a tela " + rota, e);
            tela = new Tela("Não foi possível abrir \"" + rota.getTitulo() + "\"", String.valueOf(e.getMessage())) {
            };
        }
        ToggleButton botao = botoes.get(rota);
        if (botao != null) {
            botao.setSelected(true);
        } else {
            grupo.selectToggle(null);
        }
        conteudo.getChildren().setAll(Ui.rolagem(tela));
        atualizarUsuario();
        tituloApp.setText(Contexto.CONFIG.get(ConfiguracaoService.APP_TITULO));
        atualizarSelos();
    }

    private Node criarTela(Rota rota, Object parametro) {
        Long idOs = parametro instanceof Long l ? l : null;
        return switch (rota) {
            case DASHBOARD -> new DashboardView();
            case CADASTRO -> new CadastroView(parametro instanceof OrdemServico base ? base : null);
            case ALTERAR -> new AlterarView(idOs);
            case LOCALIZAR -> new LocalizarView();
            case REABRIR -> new ReabrirView(idOs);
            case RELATORIOS -> new RelatoriosView(idOs);
            case LOTE -> new LoteView();
            case HISTORICO -> new HistoricoView(idOs);
            case ALERTAS -> new AlertasView();
            case IMPORTAR_EXPORTAR -> new ImportarExportarView();
            case PERFIL -> new PerfilView();
        };
    }

    private void atualizarSelos() {
        try {
            definirSelo(Rota.LOTE, Contexto.DASHBOARD.pendentesAssinatura());
            definirSelo(Rota.ALERTAS, Contexto.ALERTAS.quantidadeAtiva());
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Não foi possível atualizar os contadores do menu", e);
        }
    }

    private void definirSelo(Rota rota, int quantidade) {
        StackPane selo = selos.get(rota);
        ((Label) selo.getChildren().get(0)).setText(String.valueOf(quantidade));
        selo.setVisible(quantidade > 0);
        selo.setManaged(quantidade > 0);
    }
}
