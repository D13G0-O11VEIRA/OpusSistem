package br.opussistem.view;

import br.opussistem.App;
import br.opussistem.controller.Contexto;
import br.opussistem.controller.Marca;
import br.opussistem.controller.Navegador;
import br.opussistem.controller.Rota;
import br.opussistem.exception.AppException;
import br.opussistem.model.PerfilUsuario;
import br.opussistem.model.RodapeCliente;
import br.opussistem.model.Usuario;
import br.opussistem.service.ConfiguracaoService;
import br.opussistem.service.LogoService;
import br.opussistem.service.RodapeService;
import br.opussistem.service.Rotulos;
import br.opussistem.util.Formatos;
import br.opussistem.view.componentes.AbasChip;
import br.opussistem.view.componentes.Dialogos;
import br.opussistem.view.componentes.PainelAssinatura;
import br.opussistem.view.componentes.Tabela;
import br.opussistem.view.componentes.Toast;
import br.opussistem.view.componentes.Ui;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;

/**
 * Perfil do usuário (clicando no nome, no topo) e configurações do sistema. Os dados da empresa e
 * os padrões ficam no banco e alimentam o cabeçalho do relatório final e os formulários de OS.
 * Apenas administradores veem as abas de configuração.
 */
public class PerfilView extends Tela {

    private static int ultimaAba;

    private static final List<String> CAMPOS_ROTULO = List.of("cliente", "equipamento", "modelo", "fabricante", "patrimonio",
            "serie", "statusEquipamento", "manutencao", "tecnico", "natureza", "laudo", "observacoes", "valor");

    /** Controles de texto ligados a chaves de configuração. */
    private static final class Campos {
        private final Map<String, TextInputControl> mapa = new LinkedHashMap<>();

        TextField texto(String chave, String prompt) {
            TextField t = Ui.texto(prompt);
            t.setText(Contexto.CONFIG.get(chave));
            mapa.put(chave, t);
            return t;
        }

        TextArea area(String chave, int linhas) {
            TextArea a = new TextArea(Contexto.CONFIG.get(chave));
            a.setPrefRowCount(linhas);
            a.setWrapText(true);
            mapa.put(chave, a);
            return a;
        }

        Map<String, String> valores() {
            Map<String, String> m = new LinkedHashMap<>();
            mapa.forEach((chave, controle) -> m.put(chave, controle.getText() == null ? "" : controle.getText().trim()));
            return m;
        }
    }

    public PerfilView() {
        super("Perfil e configurações", "Dados do usuário, da empresa e padrões usados nas OS e no relatório final.");
        AbasChip abas = new AbasChip();
        abas.adicionar("Meu perfil", this::meuPerfil);
        if (Contexto.usuario().isAdmin()) {
            abas.adicionar("Empresa e relatório", this::empresa);
            abas.adicionar("Rodapés por cliente", this::rodapesPorCliente);
            abas.adicionar("Padrões da OS", this::padroes);
            abas.adicionar("Campos e listas", this::camposEListas);
            abas.adicionar("Aparência", this::aparencia);
            abas.adicionar("Usuários", this::usuarios);
        }
        abas.aoMudar(indice -> ultimaAba = indice);
        abas.selecionar(Math.min(ultimaAba, abas.quantidade() - 1));
        getChildren().add(abas);
    }

    // ------------------------------------------------------------ meu perfil

    private Node meuPerfil() {
        Usuario u = Contexto.usuario();
        TextField nome = Ui.texto("Nome");
        nome.setText(u.getNome());
        TextField cargo = Ui.texto("Cargo ou função");
        cargo.setText(Formatos.texto(u.getCargo()));
        TextField email = Ui.texto("E-mail");
        email.setText(Formatos.texto(u.getEmail()));
        PasswordField senha = new PasswordField();
        senha.setPromptText("deixe em branco para manter");
        PasswordField confirmacao = new PasswordField();
        confirmacao.setPromptText("repita a nova senha");

        Ui.Grade g = Ui.grade(2);
        g.adicionar(Ui.campo("Nome", nome)).adicionar(Ui.campo("Cargo / função", cargo))
                .adicionar(Ui.campo("E-mail", email)).adicionar(Ui.campo("Usuário de acesso", desabilitado(u.getUsuario())))
                .adicionar(Ui.campo("Nova senha", senha)).adicionar(Ui.campo("Confirmar nova senha", confirmacao));

        VBox dados = Ui.card("Meus dados", g, Ui.acoes(Ui.botao("Salvar perfil", () -> {
            if (Ui.valor(nome) == null) {
                throw new AppException("O nome não pode ficar vazio.");
            }
            String nova = senha.getText();
            if (!nova.isEmpty() && !nova.equals(confirmacao.getText())) {
                throw new AppException("As senhas não conferem.");
            }
            u.setNome(nome.getText().trim());
            u.setCargo(Ui.valor(cargo));
            u.setEmail(Ui.valor(email));
            Contexto.AUTH.atualizarPerfil(u, nova.isEmpty() ? null : nova);
            Toast.mostrar("Perfil salvo");
            Navegador.ir(Rota.PERFIL);
        })));

        // assinatura salva (usada com um clique ao assinar relatórios como técnico)
        PainelAssinatura painel = new PainelAssinatura();
        VBox assinatura = Ui.card("Minha assinatura",
                new Label("Salve sua assinatura para usá-la com um clique ao assinar relatórios como técnico."));
        Contexto.AUTH.lerAssinatura(u).ifPresent(bytes -> {
            ImageView atual = new ImageView(new Image(new ByteArrayInputStream(bytes), 240, 90, true, true));
            assinatura.getChildren().addAll(new Label("Assinatura salva:"), atual);
        });
        assinatura.getChildren().addAll(painel, Ui.linha(
                Ui.secundario("Limpar", painel::limpar),
                Ui.botao("Salvar assinatura", () -> {
                    if (painel.vazio()) {
                        throw new AppException("Desenhe a assinatura no quadro primeiro.");
                    }
                    Contexto.AUTH.salvarAssinatura(u, painel.png());
                    Toast.mostrar("Assinatura salva");
                    Navegador.ir(Rota.PERFIL);
                }),
                Ui.perigo("Remover assinatura salva", () -> {
                    Contexto.AUTH.removerAssinatura(u);
                    Toast.mostrar("Assinatura removida");
                    Navegador.ir(Rota.PERFIL);
                })));
        return new VBox(16, dados, assinatura);
    }

    private static TextField desabilitado(String texto) {
        TextField t = new TextField(texto);
        t.setDisable(true);
        return t;
    }

    // ------------------------------------------------------------ empresa e relatório

    private Node empresa() {
        Campos c = new Campos();
        Ui.Grade g = Ui.grade(2);
        g.adicionar(Ui.campo("Nome fantasia", c.texto(ConfiguracaoService.EMPRESA_FANTASIA, "")))
                .adicionar(Ui.campo("Razão social", c.texto(ConfiguracaoService.EMPRESA_RAZAO, "")))
                .adicionar(Ui.campo("CNPJ", c.texto(ConfiguracaoService.EMPRESA_CNPJ, "00.000.000/0000-00")))
                .adicionar(Ui.campo("Inscrição estadual", c.texto(ConfiguracaoService.EMPRESA_IE, "")))
                .adicionar(Ui.campo("Telefone", c.texto(ConfiguracaoService.EMPRESA_TELEFONE, "")))
                .adicionar(Ui.campo("E-mail", c.texto(ConfiguracaoService.EMPRESA_EMAIL, "")))
                .linhaInteira(Ui.campo("Endereço completo", c.texto(ConfiguracaoService.EMPRESA_ENDERECO, "")))
                .linhaInteira(Ui.campo("Registros e licenças (um por linha, ex.: ANVISA: 0.00.000-0)",
                        c.area(ConfiguracaoService.EMPRESA_REGISTROS, 3)));

        Ui.Grade r = Ui.grade(2);
        r.adicionar(Ui.campo("Título do relatório", c.texto(ConfiguracaoService.REL_TITULO, "")))
                .adicionar(Ui.campo("Rótulo da validade (ex.: Calibração expira)", c.texto(ConfiguracaoService.REL_VALIDADE_ROTULO, "")))
                .adicionar(Ui.campo("Assinatura 1 (rótulo)", c.texto(ConfiguracaoService.REL_ASSINATURA1, "")))
                .adicionar(Ui.campo("Assinatura 2 (rótulo)", c.texto(ConfiguracaoService.REL_ASSINATURA2, "")))
                .adicionar(Ui.campo("Validade em dias (0 oculta)", c.texto(ConfiguracaoService.REL_VALIDADE_DIAS, "365")))
                .linhaInteira(Ui.campo("Rodapé geral do relatório (vale quando não há rodapé do cliente nem da OS)", c.area(ConfiguracaoService.REL_RODAPE, 2)));

        Button salvar = Ui.botao("Salvar empresa e relatório", () -> {
            Map<String, String> valores = c.valores();
            exigirInteiro(valores.get(ConfiguracaoService.REL_VALIDADE_DIAS), "Validade em dias");
            Contexto.CONFIG.salvar(valores);
            Marca.atualizar();
            Toast.mostrar("Dados salvos");
        });
        return new VBox(16,
                Ui.card("Dados da empresa (cabeçalho do relatório)", g),
                logo(),
                Ui.card("Relatório", r),
                Ui.acoes(salvar));
    }

    /**
     * Logo da empresa. Começa vazia; depois de enviada aparece no menu lateral e no cabeçalho do relatório, nos tamanhos
     * escolhidos com as barras deslizantes (prévia ao vivo; o valor é salvo ao soltar a barra).
     */
    private VBox logo() {
        LogoService logos = Contexto.LOGO;
        Label intro = new Label("A logo da sua empresa começa vazia. Depois que você enviar uma imagem, ela aparece no menu "
                + "lateral e no cabeçalho dos relatórios. A logo do OpusSistem aparece apenas dentro do programa, "
                + "nunca nos relatórios.");
        intro.setWrapText(true);
        VBox card = Ui.card("Logo da empresa", intro);

        Optional<byte[]> atual = logos.imagem();
        if (atual.isEmpty()) {
            Label vazio = new Label("Nenhuma logo cadastrada.");
            vazio.getStyleClass().add("dica-texto");
            card.getChildren().add(vazio);
        } else {
            Image imagem = new Image(new ByteArrayInputStream(atual.get()));
            card.getChildren().addAll(
                    controleTamanho("Tamanho no menu lateral", "px", imagem, logos.tamanhoMenu(),
                            LogoService.MENU_MIN, LogoService.MENU_MAX, true, valor -> {
                                logos.salvarTamanhoMenu(valor);
                                Marca.atualizar();
                            }),
                    controleTamanho("Tamanho no relatório", "pt", imagem, logos.tamanhoRelatorio(),
                            LogoService.RELATORIO_MIN, LogoService.RELATORIO_MAX, false, logos::salvarTamanhoRelatorio));
        }

        Button escolher = Ui.secundario(atual.isPresent() ? "Trocar logo..." : "Enviar logo...", () -> {
            FileChooser escolha = new FileChooser();
            escolha.setTitle("Escolher a logo da empresa");
            escolha.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            File arquivo = escolha.showOpenDialog(Ui.janela(card));
            if (arquivo != null) {
                byte[] bytes;
                try {
                    bytes = Files.readAllBytes(arquivo.toPath());
                } catch (IOException e) {
                    throw new AppException("Não foi possível ler o arquivo: " + e.getMessage(), e);
                }
                logos.salvar(bytes);
                Marca.atualizar();
                Toast.mostrar("Logo salva");
                Navegador.ir(Rota.PERFIL);
            }
        });
        HBox botoes = new HBox(8, escolher);
        if (atual.isPresent()) {
            botoes.getChildren().add(Ui.secundario("Tamanhos padrão", () -> {
                logos.restaurarTamanhos();
                Marca.atualizar();
                Navegador.ir(Rota.PERFIL);
            }));
            botoes.getChildren().add(Ui.perigo("Remover logo", () -> {
                logos.remover();
                Marca.atualizar();
                Toast.mostrar("Logo removida");
                Navegador.ir(Rota.PERFIL);
            }));
        }
        card.getChildren().add(botoes);
        return card;
    }

    /** Barra deslizante + prévia (sobre o azul do menu ou sobre a folha branca do relatório). */
    private VBox controleTamanho(String titulo, String unidade, Image imagem, int inicial, int minimo, int maximo,
            boolean sobreMenu, IntConsumer salvar) {
        Slider barra = new Slider(minimo, maximo, inicial);
        barra.setBlockIncrement(5);
        HBox.setHgrow(barra, Priority.ALWAYS);
        barra.setMaxWidth(Double.MAX_VALUE);
        Label valor = new Label(inicial + " " + unidade);
        valor.setMinWidth(64);

        ImageView previa = new ImageView(imagem);
        previa.setPreserveRatio(true);
        previa.setSmooth(true);
        previa.setFitWidth(inicial);
        previa.setFitHeight(inicial);
        StackPane faixa = new StackPane(previa);
        faixa.setAlignment(Pos.CENTER_LEFT);
        faixa.setPadding(new Insets(12));
        faixa.setMinHeight(minimo + 24);
        faixa.setStyle(sobreMenu
                ? "-fx-background-color: -cor-menu; -fx-background-radius: 8;"
                : "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: -cor-borda; -fx-border-radius: 8;");

        Runnable salvarAgora = () -> Ui.executar(() -> salvar.accept((int) Math.round(barra.getValue())));
        barra.valueProperty().addListener((obs, antes, depois) -> {
            int v = (int) Math.round(depois.doubleValue());
            valor.setText(v + " " + unidade);
            previa.setFitWidth(v);
            previa.setFitHeight(v);
            if (!barra.isValueChanging()) {
                salvarAgora.run();
            }
        });
        barra.valueChangingProperty().addListener((obs, antes, mudando) -> {
            if (!mudando) {
                salvarAgora.run();
            }
        });

        Label dica = new Label(minimo + " a " + maximo + " " + unidade + " no maior lado da imagem");
        dica.getStyleClass().add("dica-texto");
        return new VBox(6, Ui.campo(titulo, new HBox(10, barra, valor)), dica, faixa);
    }

    // ------------------------------------------------------------ rodapés por cliente

    private Node rodapesPorCliente() {
        Tabela<RodapeCliente> tabela = new Tabela<RodapeCliente>()
                .col("Cliente", RodapeCliente::cliente, 230)
                .col("Texto do rodapé", r -> primeiraLinha(r.texto()), 430)
                .itens(Contexto.RODAPES.listar())
                .linhasVisiveis(6);

        ComboBox<String> cliente = Ui.comboEditavel(Contexto.OS.sugestoes("cliente", List.of()));
        TextArea texto = new TextArea();
        texto.setPrefRowCount(4);
        texto.setWrapText(true);
        texto.setPromptText("Ex.: Dúvidas sobre este relatório: compras@empresa-do-cliente.com");
        Label contador = new Label("0 / " + RodapeService.LIMITE_CARACTERES);
        contador.getStyleClass().add("dica-texto");
        texto.textProperty().addListener((obs, antes, depois) ->
                contador.setText(depois.length() + " / " + RodapeService.LIMITE_CARACTERES));
        tabela.aoSelecionar(r -> {
            if (r != null) {
                Ui.definir(cliente, r.cliente());
                texto.setText(r.texto());
            }
        });

        Label explicacao = new Label("Alguns clientes pedem um texto fixo no pé da folha (por exemplo, um e-mail para contato). "
                + "Cadastre aqui e ele sairá no rodapé de todos os relatórios desse cliente. "
                + "O rodapé de uma OS específica (tela Relatórios) tem prioridade; sem nenhum dos dois, vale o rodapé geral.");
        explicacao.setWrapText(true);
        explicacao.getStyleClass().add("dica-texto");

        VBox lista = Ui.card("Clientes com rodapé próprio", explicacao, tabela.view(), Ui.acoes(Ui.perigo("Excluir selecionado", () -> {
            RodapeCliente sel = tabela.selecionado();
            if (sel == null) {
                Toast.mostrar("Selecione um cliente na lista.");
            } else if (Dialogos.confirmar("Remover o rodapé do cliente " + sel.cliente() + "?")) {
                Contexto.RODAPES.removerDoCliente(sel.cliente());
                Toast.mostrar("Rodapé removido");
                Navegador.ir(Rota.PERFIL);
            }
        })));
        VBox edicao = Ui.card("Adicionar ou editar rodapé de um cliente",
                Ui.campo("Cliente (use o mesmo nome das OS)", cliente),
                Ui.campo("Texto do rodapé", texto), contador,
                Ui.acoes(Ui.secundario("Limpar", () -> {
                    Ui.definir(cliente, null);
                    texto.clear();
                }), Ui.botao("Salvar rodapé do cliente", () -> {
                    Contexto.RODAPES.salvarDoCliente(Ui.valor(cliente), texto.getText());
                    Toast.mostrar("Rodapé do cliente salvo");
                    Navegador.ir(Rota.PERFIL);
                })));
        return new VBox(16, lista, edicao);
    }

    private static String primeiraLinha(String texto) {
        if (texto == null) {
            return "";
        }
        String[] linhas = texto.split("\\R", 2);
        return linhas.length > 1 ? linhas[0] + " …" : linhas[0];
    }

    // ------------------------------------------------------------ padrões da OS

    private Node padroes() {
        Campos c = new Campos();
        Ui.Grade g = Ui.grade(2);
        g.adicionar(Ui.campo(Rotulos.de("equipamento"), c.texto(ConfiguracaoService.PAD_EQUIPAMENTO, "")))
                .adicionar(Ui.campo(Rotulos.de("fabricante"), c.texto(ConfiguracaoService.PAD_FABRICANTE, "")))
                .adicionar(Ui.campo(Rotulos.de("tecnico") + " (vazio = usuário logado)", c.texto(ConfiguracaoService.PAD_TECNICO, "")))
                .adicionar(Ui.campo(Rotulos.de("manutencao"), c.texto(ConfiguracaoService.PAD_MANUTENCAO, "ex.: Preventiva")))
                .adicionar(Ui.campo(Rotulos.de("statusEquipamento"), c.texto(ConfiguracaoService.PAD_STATUS_EQUIPAMENTO, "ex.: Operante")))
                .adicionar(Ui.campo("Dias até a próxima preventiva (0 = não preencher)", c.texto(ConfiguracaoService.PAD_DIAS_PREVENTIVA, "180")))
                .linhaInteira(Ui.campo(Rotulos.de("natureza"), c.area(ConfiguracaoService.PAD_NATUREZA, 3)))
                .linhaInteira(Ui.campo(Rotulos.de("laudo"), c.area(ConfiguracaoService.PAD_LAUDO, 3)))
                .linhaInteira(Ui.campo(Rotulos.de("observacoes"), c.area(ConfiguracaoService.PAD_OBSERVACOES, 2)));
        return new VBox(16, Ui.card("Valores que já vêm preenchidos ao abrir uma nova OS", g),
                Ui.acoes(Ui.botao("Salvar padrões", () -> {
                    Map<String, String> valores = c.valores();
                    exigirInteiro(valores.get(ConfiguracaoService.PAD_DIAS_PREVENTIVA), "Dias até a próxima preventiva");
                    Contexto.CONFIG.salvar(valores);
                    Toast.mostrar("Padrões salvos");
                })));
    }

    // ------------------------------------------------------------ campos e listas

    private Node camposEListas() {
        Campos c = new Campos();
        Ui.Grade nomes = Ui.grade(3);
        for (String campo : CAMPOS_ROTULO) {
            nomes.adicionar(Ui.campo(Rotulos.padroes().get(campo), c.texto("rotulo." + campo, Rotulos.padroes().get(campo))));
        }
        Label dica = new Label("Renomeie os campos para o seu segmento (por exemplo: Patrimônio → Placa, Equipamento → Veículo). "
                + "Deixe em branco para usar o nome padrão. Os novos nomes valem nas telas e no relatório.");
        dica.setWrapText(true);
        dica.getStyleClass().add("dica-texto");

        Ui.Grade listas = Ui.grade(2);
        listas.adicionar(Ui.campo("Equipamentos (um por linha)", c.area(ConfiguracaoService.LISTA_EQUIPAMENTOS, 6)))
                .adicionar(Ui.campo("Fabricantes / marcas", c.area(ConfiguracaoService.LISTA_FABRICANTES, 6)))
                .adicionar(Ui.campo("Técnicos", c.area(ConfiguracaoService.LISTA_TECNICOS, 6)))
                .adicionar(Ui.campo("Tipos de manutenção", c.area(ConfiguracaoService.LISTA_MANUTENCAO, 6)))
                .adicionar(Ui.campo("Status do equipamento", c.area(ConfiguracaoService.LISTA_STATUS_EQUIPAMENTO, 6)));
        Label dicaListas = new Label("Estas listas viram sugestões nos formulários (você também pode digitar valores novos).");
        dicaListas.getStyleClass().add("dica-texto");

        return new VBox(16, Ui.card("Nomes dos campos", dica, nomes), Ui.card("Listas de sugestões", dicaListas, listas),
                Ui.acoes(Ui.botao("Salvar campos e listas", () -> {
                    Contexto.CONFIG.salvar(c.valores());
                    Toast.mostrar("Campos e listas salvos");
                })));
    }

    // ------------------------------------------------------------ aparência

    private Node aparencia() {
        ConfiguracaoService cfg = Contexto.CONFIG;
        Campos c = new Campos();
        TextField titulo = c.texto(ConfiguracaoService.APP_TITULO, "");
        ComboBox<String> tema = new ComboBox<>(FXCollections.observableArrayList("Claro", "Escuro"));
        tema.setValue("ESCURO".equalsIgnoreCase(cfg.get(ConfiguracaoService.TEMA)) ? "Escuro" : "Claro");
        ColorPicker destaque = new ColorPicker(Color.web(cfg.get(ConfiguracaoService.COR_DESTAQUE)));
        ColorPicker menu = new ColorPicker(Color.web(cfg.get(ConfiguracaoService.COR_MENU)));

        Ui.Grade g = Ui.grade(2);
        g.linhaInteira(Ui.campo("Título do sistema (barra superior)", titulo))
                .adicionar(Ui.campo("Tema", tema)).adicionar(Ui.campo("Cor de destaque", destaque))
                .adicionar(Ui.campo("Cor do menu lateral", menu));
        Label dados = new Label("Os dados ficam no banco MySQL. Para backup, use Exportar / Importar (JSON) ou o mysqldump do servidor.");
        dados.setWrapText(true);
        dados.getStyleClass().add("dica-texto");
        return new VBox(16, Ui.card("Aparência", g, dados), Ui.acoes(
                Ui.secundario("Restaurar cores padrão", () -> {
                    destaque.setValue(Color.web("#2f6fe4"));
                    menu.setValue(Color.web("#0d2a63"));
                }),
                Ui.botao("Salvar aparência", () -> {
                    Map<String, String> m = new HashMap<>(c.valores());
                    m.put(ConfiguracaoService.TEMA, "Escuro".equals(tema.getValue()) ? "ESCURO" : "CLARO");
                    m.put(ConfiguracaoService.COR_DESTAQUE, hex(destaque.getValue()));
                    m.put(ConfiguracaoService.COR_MENU, hex(menu.getValue()));
                    cfg.salvar(m);
                    App.reaplicarTema();
                    Toast.mostrar("Aparência atualizada");
                })));
    }

    private static String hex(Color cor) {
        return String.format("#%02x%02x%02x", Math.round(cor.getRed() * 255), Math.round(cor.getGreen() * 255),
                Math.round(cor.getBlue() * 255));
    }

    // ------------------------------------------------------------ usuários

    private Node usuarios() {
        Tabela<Usuario> tabela = new Tabela<Usuario>()
                .col("Nome", Usuario::getNome, 200)
                .col("Usuário", Usuario::getUsuario, 140)
                .col("Perfil", u -> u.getPerfil().toString(), 130)
                .col("E-mail", u -> Formatos.texto(u.getEmail()), 220)
                .itens(Contexto.AUTH.listar());
        tabela.view().setPrefHeight(220);

        TextField nome = Ui.texto("Nome completo");
        TextField login = Ui.texto("usuário de acesso");
        PasswordField senha = new PasswordField();
        senha.setPromptText("mínimo de 6 caracteres");
        ComboBox<PerfilUsuario> perfil = new ComboBox<>(FXCollections.observableArrayList(PerfilUsuario.values()));
        perfil.setValue(PerfilUsuario.OPERADOR);

        Ui.Grade g = Ui.grade(4);
        g.adicionar(Ui.campo("Nome", nome)).adicionar(Ui.campo("Usuário", login))
                .adicionar(Ui.campo("Senha", senha)).adicionar(Ui.campo("Perfil", perfil));

        return new VBox(16,
                Ui.card("Usuários cadastrados", tabela.view(), Ui.linha(Ui.perigo("Excluir selecionado", () -> {
                    Usuario sel = tabela.selecionado();
                    if (sel == null) {
                        Toast.mostrar("Selecione um usuário na lista.");
                    } else if (Dialogos.confirmar("Excluir o usuário " + sel.getUsuario() + "?")) {
                        Contexto.AUTH.excluir(sel.getId(), Contexto.usuario());
                        Toast.mostrar("Usuário excluído");
                        Navegador.ir(Rota.PERFIL);
                    }
                }))),
                Ui.card("Novo usuário", g, Ui.acoes(Ui.botao("Criar usuário", () -> {
                    Contexto.AUTH.cadastrar(nome.getText(), login.getText(), null, senha.getText(), senha.getText(), perfil.getValue());
                    Toast.mostrar("Usuário criado");
                    Navegador.ir(Rota.PERFIL);
                }))));
    }

    private static void exigirInteiro(String valor, String campo) {
        if (valor != null && !valor.isBlank()) {
            try {
                Integer.parseInt(valor.trim());
            } catch (NumberFormatException e) {
                throw new AppException(campo + " deve ser um número inteiro.");
            }
        }
    }
}
