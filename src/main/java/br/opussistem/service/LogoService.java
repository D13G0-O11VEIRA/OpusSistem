package br.opussistem.service;

import br.opussistem.exception.AppException;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Logo da empresa do usuário. Começa VAZIA: só existe depois que o usuário a envia em
 * Perfil > Empresa e relatório. Fica guardada no banco (junto das demais configurações), por isso uma instalação
 * nova ou um banco novo nunca herda a logo de outra instalação. Também guarda o tamanho da logo no menu lateral e
 * no relatório (controles deslizantes do Perfil). A logo do OpusSistem é outra coisa: só aparece dentro do programa.
 */
public class LogoService {

    public static final String CHAVE_IMAGEM = "empresa.logo";
    public static final String CHAVE_MENU = "empresa.logo.menu";
    public static final String CHAVE_RELATORIO = "empresa.logo.relatorio";

    /** Maior lado da logo, em pixels, no menu lateral. */
    public static final int MENU_MIN = 40;
    public static final int MENU_MAX = 190;
    public static final int MENU_PADRAO = 110;

    /** Maior lado da logo, em pontos (1 pt = 1/72 de polegada), no relatório. */
    public static final int RELATORIO_MIN = 30;
    public static final int RELATORIO_MAX = 180;
    public static final int RELATORIO_PADRAO = 80;

    static final int LADO_MAXIMO_PX = 800;
    static final long TAMANHO_MAXIMO_ARQUIVO = 5L * 1024 * 1024;

    private final ConfiguracaoService config = ConfiguracaoService.instancia();

    public boolean existe() {
        return !config.get(CHAVE_IMAGEM).isBlank();
    }

    /** PNG da logo, se o usuário já enviou uma. */
    public Optional<byte[]> imagem() {
        String base64 = config.get(CHAVE_IMAGEM);
        if (base64.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Base64.getDecoder().decode(base64));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Aceita PNG, JPG, GIF ou BMP; guarda como PNG de no máximo 800 px no maior lado. */
    public void salvar(byte[] original) {
        byte[] png = normalizar(original);
        config.salvar(CHAVE_IMAGEM, Base64.getEncoder().encodeToString(png));
    }

    public void remover() {
        config.salvar(CHAVE_IMAGEM, "");
    }

    public int tamanhoMenu() {
        return limitar(config.getInt(CHAVE_MENU, MENU_PADRAO), MENU_MIN, MENU_MAX);
    }

    public int tamanhoRelatorio() {
        return limitar(config.getInt(CHAVE_RELATORIO, RELATORIO_PADRAO), RELATORIO_MIN, RELATORIO_MAX);
    }

    public void salvarTamanhoMenu(int valor) {
        config.salvar(CHAVE_MENU, String.valueOf(limitar(valor, MENU_MIN, MENU_MAX)));
    }

    public void salvarTamanhoRelatorio(int valor) {
        config.salvar(CHAVE_RELATORIO, String.valueOf(limitar(valor, RELATORIO_MIN, RELATORIO_MAX)));
    }

    public void restaurarTamanhos() {
        config.salvar(Map.of(CHAVE_MENU, String.valueOf(MENU_PADRAO), CHAVE_RELATORIO, String.valueOf(RELATORIO_PADRAO)));
    }

    // ------------------------------------------------------------------ apoio (sem banco)

    static int limitar(int valor, int minimo, int maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }

    static byte[] normalizar(byte[] original) {
        if (original == null || original.length == 0) {
            throw new AppException("Escolha uma imagem para a logo.");
        }
        if (original.length > TAMANHO_MAXIMO_ARQUIVO) {
            throw new AppException("A imagem passa de 5 MB. Escolha um arquivo menor.");
        }
        BufferedImage imagem;
        try {
            imagem = ImageIO.read(new ByteArrayInputStream(original));
        } catch (IOException | RuntimeException e) {
            throw new AppException("Não foi possível ler a imagem. Use PNG, JPG, GIF ou BMP.", e);
        }
        if (imagem == null) {
            throw new AppException("Não foi possível ler a imagem. Use PNG, JPG, GIF ou BMP.");
        }
        double escala = Math.min(1.0, (double) LADO_MAXIMO_PX / Math.max(imagem.getWidth(), imagem.getHeight()));
        int largura = Math.max(1, (int) Math.round(imagem.getWidth() * escala));
        int altura = Math.max(1, (int) Math.round(imagem.getHeight() * escala));
        BufferedImage saida = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = saida.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(imagem, 0, 0, largura, altura, null);
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            ImageIO.write(saida, "png", bytes);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new AppException("Não foi possível processar a imagem.", e);
        }
    }
}
