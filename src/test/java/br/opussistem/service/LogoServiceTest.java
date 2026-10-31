package br.opussistem.service;

import br.opussistem.exception.AppException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogoServiceTest {

    private static byte[] png(int largura, int altura) throws IOException {
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(img, "png", saida);
        return saida.toByteArray();
    }

    private static BufferedImage ler(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    @Test
    void imagemGrandeEReduzidaMantendoAProporcao() throws IOException {
        BufferedImage resultado = ler(LogoService.normalizar(png(1600, 400)));
        assertEquals(800, resultado.getWidth());
        assertEquals(200, resultado.getHeight());
    }

    @Test
    void imagemPequenaNaoEAmpliada() throws IOException {
        BufferedImage resultado = ler(LogoService.normalizar(png(120, 60)));
        assertEquals(120, resultado.getWidth());
        assertEquals(60, resultado.getHeight());
    }

    @Test
    void arquivoQueNaoEImagemEhRejeitado() {
        assertThrows(AppException.class, () -> LogoService.normalizar("isto não é uma imagem".getBytes()));
        assertThrows(AppException.class, () -> LogoService.normalizar(new byte[0]));
        assertThrows(AppException.class, () -> LogoService.normalizar(null));
    }

    @Test
    void arquivoMuitoGrandeEhRejeitado() {
        byte[] enorme = new byte[(int) LogoService.TAMANHO_MAXIMO_ARQUIVO + 1];
        assertThrows(AppException.class, () -> LogoService.normalizar(enorme));
    }

    @Test
    void tamanhosFicamDentroDosLimites() {
        assertEquals(LogoService.MENU_MIN, LogoService.limitar(5, LogoService.MENU_MIN, LogoService.MENU_MAX));
        assertEquals(LogoService.MENU_MAX, LogoService.limitar(999, LogoService.MENU_MIN, LogoService.MENU_MAX));
        assertEquals(100, LogoService.limitar(100, LogoService.MENU_MIN, LogoService.MENU_MAX));
        assertTrue(LogoService.RELATORIO_MIN < LogoService.RELATORIO_PADRAO
                && LogoService.RELATORIO_PADRAO < LogoService.RELATORIO_MAX);
        assertTrue(LogoService.MENU_MIN < LogoService.MENU_PADRAO && LogoService.MENU_PADRAO < LogoService.MENU_MAX);
    }
}
