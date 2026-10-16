package br.opussistem.view.componentes;

import br.opussistem.exception.AppException;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.SnapshotParameters;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Quadro para desenhar a assinatura com o mouse, caneta ou toque. */
public class PainelAssinatura extends StackPane {

    private static final double LARGURA = 460;
    private static final double ALTURA = 150;

    private final Canvas canvas = new Canvas(LARGURA, ALTURA);
    private final Label dica = new Label("Assine aqui");
    private boolean vazio = true;
    private double ultimoX;
    private double ultimoY;

    public PainelAssinatura() {
        getStyleClass().add("painel-assinatura");
        dica.getStyleClass().add("dica-assinatura");
        dica.setMouseTransparent(true);
        getChildren().addAll(canvas, dica);
        setPrefSize(LARGURA, ALTURA);
        setMaxSize(LARGURA, ALTURA);
        setMinSize(LARGURA, ALTURA);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(2.4);
        gc.setStroke(Color.web("#0b1b3a"));
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        canvas.setOnMousePressed(e -> {
            ultimoX = e.getX();
            ultimoY = e.getY();
            gc.beginPath();
            gc.moveTo(ultimoX, ultimoY);
            gc.lineTo(ultimoX + 0.1, ultimoY + 0.1);
            gc.stroke();
            marcarComoPreenchido();
        });
        canvas.setOnMouseDragged(e -> {
            gc.beginPath();
            gc.moveTo(ultimoX, ultimoY);
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
            ultimoX = e.getX();
            ultimoY = e.getY();
        });
    }

    private void marcarComoPreenchido() {
        vazio = false;
        dica.setVisible(false);
    }

    public boolean vazio() {
        return vazio;
    }

    public void limpar() {
        canvas.getGraphicsContext2D().clearRect(0, 0, LARGURA, ALTURA);
        vazio = true;
        dica.setVisible(true);
    }

    /** Desenha uma assinatura já salva (PNG) no quadro. */
    public void carregar(byte[] png) {
        Image img = new Image(new ByteArrayInputStream(png));
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, LARGURA, ALTURA);
        double k = Math.min(LARGURA / img.getWidth(), ALTURA / img.getHeight());
        gc.drawImage(img, (LARGURA - img.getWidth() * k) / 2, (ALTURA - img.getHeight() * k) / 2,
                img.getWidth() * k, img.getHeight() * k);
        marcarComoPreenchido();
    }

    /** PNG com fundo transparente. */
    public byte[] png() {
        SnapshotParameters parametros = new SnapshotParameters();
        parametros.setFill(Color.TRANSPARENT);
        WritableImage imagem = canvas.snapshot(parametros, null);
        int largura = (int) imagem.getWidth();
        int altura = (int) imagem.getHeight();
        BufferedImage saida = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        PixelReader leitor = imagem.getPixelReader();
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                saida.setRGB(x, y, leitor.getArgb(x, y));
            }
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            ImageIO.write(saida, "png", bytes);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new AppException("Não foi possível converter a assinatura em imagem.", e);
        }
    }
}
