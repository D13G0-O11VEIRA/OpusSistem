package br.opussistem.service;

import br.opussistem.exception.AppException;
import br.opussistem.model.Assinatura;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.PapelAssinatura;
import br.opussistem.util.Armazenamento;
import br.opussistem.util.Formatos;
import br.opussistem.util.Quebra;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gera o "Relatório de Atendimento Técnico" em PDF (A4): cabeçalho da empresa, dados da OS,
 * natureza do serviço e laudo (com a formatação do editor), assinaturas e, no pé da página, o
 * texto de rodapé (da OS, do cliente ou o geral). A observação NÃO é impressa: aparece só no sistema.
 */
public class RelatorioService {

    /** Altura reservada ao bloco de assinaturas (assinatura, validade e código de verificação). */
    private static final float RESERVA_ASSINATURAS = 135f;
    private static final float TAMANHO_RODAPE = 8f;
    private static final float ENTRELINHA_RODAPE = 10f;
    private static final int MAX_LINHAS_RODAPE = 8;
    private static final Color CINZA = new Color(90, 90, 90);

    @FunctionalInterface
    private interface ConteudoPdf {
        void escrever(Document documento, PdfWriter escritor) throws DocumentException, IOException;
    }

    private final ConfiguracaoService config = ConfiguracaoService.instancia();
    private final AssinaturaService assinaturas;
    private final RodapeService rodapes;
    private final LogoService logos;

    public RelatorioService(AssinaturaService assinaturas, RodapeService rodapes, LogoService logos) {
        this.assinaturas = assinaturas;
        this.rodapes = rodapes;
        this.logos = logos;
    }

    /** Gera storage/relatorios/&lt;numero&gt;.pdf e devolve o caminho. */
    public Path gerar(OrdemServico os) {
        Path destino = Armazenamento.relatorios().resolve(Armazenamento.nomeSeguro(os.getNumero()) + ".pdf");
        criar(destino, (doc, w) -> paginaDaOS(doc, w, os));
        return destino;
    }

    /** Capa do pacote + relatório de cada OS (todas do mesmo cliente). */
    public Path gerarPacote(String cliente, List<OrdemServico> lista, String codigoPacote) {
        if (lista.isEmpty()) {
            throw new AppException("O pacote não tem OS.");
        }
        String nome = "pacote-" + Armazenamento
                .nomeSeguro(codigoPacote == null ? String.valueOf(System.currentTimeMillis()) : codigoPacote) + ".pdf";
        Path destino = Armazenamento.relatorios().resolve(nome);
        criar(destino, (doc, w) -> {
            capa(doc, w, cliente, lista);
            for (OrdemServico os : lista) {
                doc.newPage();
                paginaDaOS(doc, w, os);
            }
        });
        return destino;
    }

    // ------------------------------------------------------------------ documento

    private void criar(Path destino, ConteudoPdf conteudo) {
        Armazenamento.garantir(destino.getParent());
        Document doc = new Document(PageSize.A4, 40, 40, 36, 36);
        try (OutputStream out = Files.newOutputStream(destino)) {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.addTitle(config.get(ConfiguracaoService.REL_TITULO));
            doc.addCreator("OpusSistem");
            doc.open();
            conteudo.escrever(doc, writer);
            doc.close(); // precisa fechar antes de o arquivo ser fechado
        } catch (IOException | RuntimeException e) { // DocumentException é RuntimeException no OpenPDF 2.x
            Armazenamento.apagarSilencioso(destino);
            throw new AppException("Não foi possível gerar o PDF: " + e.getMessage(), e);
        }
    }

    private void paginaDaOS(Document doc, PdfWriter writer, OrdemServico os) throws DocumentException, IOException {
        cabecalho(doc);

        Paragraph titulo = new Paragraph(config.get(ConfiguracaoService.REL_TITULO), fonte(15, Font.BOLD, Color.BLACK));
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingBefore(12);
        titulo.setSpacingAfter(10);
        doc.add(titulo);

        doc.add(tabelaDeDados(os));

        secao(doc, Rotulos.de("natureza"), os.getDescricaoProblema());
        secao(doc, Rotulos.de("laudo"), os.getLaudoTecnico());
        // Observação: de propósito não é impressa.

        if (os.getValorFinal() != null && os.getValorFinal().signum() > 0) {
            Paragraph valor = new Paragraph(Rotulos.de("valor") + ": " + Formatos.moeda(os.getValorFinal()),
                    fonte(10, Font.BOLD, Color.BLACK));
            valor.setSpacingBefore(8);
            doc.add(valor);
        }

        Map<PapelAssinatura, Assinatura> ass = assinaturas.daOS(os.getId());
        PdfPTable bloco = blocoAssinaturas(os, ass.get(PapelAssinatura.CLIENTE), ass.get(PapelAssinatura.TECNICO), true);
        posicionarNoRodape(doc, writer, bloco, rodapes.efetivo(os).texto());
    }

    private void capa(Document doc, PdfWriter writer, String cliente, List<OrdemServico> lista)
            throws DocumentException, IOException {
        cabecalho(doc);

        Paragraph titulo = new Paragraph("Pacote de Relatórios de Atendimento Técnico", fonte(16, Font.BOLD, Color.BLACK));
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingBefore(30);
        doc.add(titulo);
        Paragraph nome = new Paragraph(cliente, fonte(22, Font.BOLD, Color.BLACK));
        nome.setAlignment(Element.ALIGN_CENTER);
        nome.setSpacingBefore(12);
        doc.add(nome);
        Paragraph qtd = new Paragraph(
                lista.size() + (lista.size() == 1 ? " ordem de serviço" : " ordens de serviço"),
                fonte(11, Font.NORMAL, CINZA));
        qtd.setAlignment(Element.ALIGN_CENTER);
        qtd.setSpacingAfter(20);
        doc.add(qtd);

        PdfPTable t = new PdfPTable(new float[] { 14, 34, 18, 16, 28 });
        t.setWidthPercentage(100);
        for (String h : new String[] { "Nº OS", Rotulos.de("equipamento"), Rotulos.de("patrimonio"), "Data",
                Rotulos.de("tecnico") }) {
            PdfPCell c = new PdfPCell(new Phrase(h, fonte(9, Font.BOLD, Color.BLACK)));
            c.setBackgroundColor(new Color(235, 238, 245));
            c.setPadding(4);
            t.addCell(c);
        }
        for (OrdemServico os : lista) {
            for (String v : new String[] { os.getNumero(), Formatos.texto(os.getEquipamento()),
                    Formatos.texto(os.getPatrimonio()), Formatos.data(os.dataReferencia()),
                    Formatos.texto(os.getTecnico()) }) {
                PdfPCell c = new PdfPCell(new Phrase(v, fonte(9, Font.NORMAL, Color.BLACK)));
                c.setPadding(4);
                t.addCell(c);
            }
        }
        doc.add(t);

        Paragraph texto = new Paragraph("Com a assinatura abaixo, o cliente confirma o recebimento e a ciência dos "
                + lista.size() + " relatório" + (lista.size() == 1 ? "" : "s") + " deste pacote.",
                fonte(10, Font.NORMAL, Color.BLACK));
        texto.setSpacingBefore(14);
        doc.add(texto);

        Assinatura assinaturaCliente = lista.stream()
                .map(o -> assinaturas.daOS(o.getId()).get(PapelAssinatura.CLIENTE))
                .filter(a -> a != null).findFirst().orElse(null);
        posicionarNoRodape(doc, writer, blocoAssinaturas(null, assinaturaCliente, null, false),
                rodapes.efetivoDoCliente(cliente).texto());
    }

    // ------------------------------------------------------------------ partes

    private void cabecalho(Document doc) throws DocumentException, IOException {
        ConfiguracaoService c = config;
        List<Phrase> linhas = new ArrayList<>();
        adicionar(linhas, "Razão Social", c.get(ConfiguracaoService.EMPRESA_RAZAO), null, null, null, null);
        adicionar(linhas, "Fantasia", c.get(ConfiguracaoService.EMPRESA_FANTASIA),
                "Telefone", c.get(ConfiguracaoService.EMPRESA_TELEFONE), "E-mail",
                c.get(ConfiguracaoService.EMPRESA_EMAIL));
        adicionar(linhas, "Endereço", c.get(ConfiguracaoService.EMPRESA_ENDERECO), null, null, null, null);
        adicionar(linhas, "CNPJ", c.get(ConfiguracaoService.EMPRESA_CNPJ),
                "Inscrição Estadual", c.get(ConfiguracaoService.EMPRESA_IE), null, null);
        for (String registro : c.get(ConfiguracaoService.EMPRESA_REGISTROS).split("\\R")) {
            if (!registro.isBlank()) {
                linhas.add(new Phrase(registro.trim(), fonte(9, Font.NORMAL, Color.BLACK)));
            }
        }
        // A logo é opcional: só sai no relatório se o usuário a enviou em Perfil > Empresa e relatório.
        Image img = null;
        if (logos.existe()) {
            try {
                float lado = logos.tamanhoRelatorio();
                img = Image.getInstance(logos.imagem().orElseThrow());
                img.scaleToFit(lado, lado);
            } catch (RuntimeException | IOException e) {
                img = null; // imagem inválida: o relatório sai sem logo
            }
        }
        boolean temLogo = img != null;
        if (linhas.isEmpty() && !temLogo) {
            return;
        }
        float larguraUtil = doc.right() - doc.left();
        float colunaLogo = temLogo ? Math.min(logos.tamanhoRelatorio() + 14f, larguraUtil * 0.6f) : 0;
        PdfPTable t = new PdfPTable(temLogo ? new float[] { colunaLogo, larguraUtil - colunaLogo } : new float[] { 100 });
        t.setWidthPercentage(100);
        if (temLogo) {
            PdfPCell cl = new PdfPCell(img, false);
            cl.setBorder(Rectangle.BOTTOM);
            cl.setPaddingBottom(6);
            t.addCell(cl);
        }
        PdfPCell texto = new PdfPCell();
        texto.setBorder(Rectangle.BOTTOM);
        texto.setPaddingBottom(6);
        for (Phrase linha : linhas) {
            Paragraph p = new Paragraph(linha);
            p.setLeading(12);
            texto.addElement(p);
        }
        if (linhas.isEmpty()) {
            texto.addElement(new Paragraph(" "));
        }
        t.addCell(texto);
        doc.add(t);
    }

    private void adicionar(List<Phrase> linhas, String r1, String v1, String r2, String v2, String r3, String v3) {
        Phrase f = new Phrase();
        boolean algum = false;
        String[][] pares = { { r1, v1 }, { r2, v2 }, { r3, v3 } };
        for (String[] par : pares) {
            if (par[0] != null && par[1] != null && !par[1].isBlank()) {
                if (algum) {
                    f.add(new Chunk("  ·  ", fonte(9, Font.NORMAL, CINZA)));
                }
                f.add(new Chunk(par[0] + ": ", fonte(9, Font.BOLD, Color.BLACK)));
                f.add(new Chunk(par[1].trim(), fonte(9, Font.NORMAL, Color.BLACK)));
                algum = true;
            }
        }
        if (algum) {
            linhas.add(f);
        }
    }

    private PdfPTable tabelaDeDados(OrdemServico os) {
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        List<String[]> pares = new ArrayList<>();
        pares.add(new String[] { "Nº OS", os.getNumero() });
        pares.add(new String[] { Rotulos.de("cliente"), os.getCliente() });
        pares.add(new String[] { "Data", Formatos.data(os.dataReferencia()) });
        pares.add(new String[] { Rotulos.de("equipamento"), os.getEquipamento() });
        pares.add(new String[] { Rotulos.de("fabricante"), os.getFabricante() });
        pares.add(new String[] { Rotulos.de("modelo"), os.getModelo() });
        pares.add(new String[] { Rotulos.de("patrimonio"), os.getPatrimonio() });
        pares.add(new String[] { Rotulos.de("serie"), os.getNumeroSerie() });
        pares.add(new String[] { Rotulos.de("tecnico"), os.getTecnico() });
        int n = 0;
        for (String[] par : pares) {
            if (par[1] == null || par[1].isBlank()) {
                continue;
            }
            Phrase f = new Phrase();
            f.add(new Chunk(par[0] + ": ", fonte(9, Font.BOLD, Color.BLACK)));
            f.add(new Chunk(par[1], fonte(9, Font.NORMAL, Color.BLACK)));
            PdfPCell c = new PdfPCell(f);
            c.setPadding(4);
            c.setBorderWidth(0.6f);
            t.addCell(c);
            n++;
        }
        while (n % 3 != 0) {
            PdfPCell vazia = new PdfPCell(new Phrase(" "));
            vazia.setBorderWidth(0.6f);
            t.addCell(vazia);
            n++;
        }
        return t;
    }

    private void secao(Document doc, String titulo, String html) throws DocumentException {
        Paragraph p = new Paragraph(titulo, fonte(11, Font.BOLD, Color.BLACK));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(12);
        p.setSpacingAfter(4);
        doc.add(p);

        PdfPTable caixa = new PdfPTable(1);
        caixa.setWidthPercentage(100);
        PdfPCell celula = new PdfPCell();
        celula.setPadding(6);
        celula.setBorderWidth(0.6f);
        for (Element e : ConversorHtmlPdf.converter(html, 10f)) {
            celula.addElement(e);
        }
        caixa.addCell(celula);
        doc.add(caixa);
    }

    /** Assinaturas, validade e código de verificação (o texto de rodapé é desenhado à parte, no pé da página). */
    private PdfPTable blocoAssinaturas(OrdemServico os, Assinatura cliente, Assinatura tecnico, boolean mostrarTecnico)
            throws DocumentException, IOException {
        PdfPTable t = new PdfPTable(new float[] { 45, 10, 45 });
        t.setWidthPercentage(100);

        t.addCell(celulaImagem(cliente));
        t.addCell(semBorda(new PdfPCell()));
        t.addCell(mostrarTecnico ? celulaImagem(tecnico) : semBorda(new PdfPCell()));

        t.addCell(celulaRotulo(config.get(ConfiguracaoService.REL_ASSINATURA1), true));
        t.addCell(semBorda(new PdfPCell()));
        t.addCell(celulaRotulo(config.get(ConfiguracaoService.REL_ASSINATURA2), mostrarTecnico));

        t.addCell(celulaDetalhe(cliente, os));
        t.addCell(semBorda(new PdfPCell()));
        t.addCell(mostrarTecnico ? celulaDetalhe(tecnico, os) : semBorda(new PdfPCell()));

        int dias = config.getInt(ConfiguracaoService.REL_VALIDADE_DIAS, 0);
        if (os != null && dias > 0) {
            LocalDateTime base = os.dataReferencia();
            String validade = config.get(ConfiguracaoService.REL_VALIDADE_ROTULO) + ": "
                    + Formatos.data(base.plusDays(dias));
            t.addCell(linhaCompleta(validade, fonte(10, Font.BOLD, Color.BLACK), 12));
        }
        Assinatura referencia = cliente != null ? cliente : tecnico;
        if (referencia != null) {
            t.addCell(linhaCompleta("Código de verificação: " + referencia.getCodigoVerificacao(),
                    fonte(7, Font.NORMAL, CINZA), 6));
        }
        return t;
    }

    private PdfPCell celulaImagem(Assinatura a) throws DocumentException, IOException {
        PdfPCell c = semBorda(new PdfPCell());
        c.setFixedHeight(56);
        c.setVerticalAlignment(Element.ALIGN_BOTTOM);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (a != null) {
            assinaturas.imagem(a).ifPresent(bytes -> {
                try {
                    Image img = Image.getInstance(bytes);
                    img.scaleToFit(150, 52);
                    img.setAlignment(Image.ALIGN_CENTER);
                    c.addElement(img);
                } catch (DocumentException | IOException e) {
                    // imagem corrompida: a assinatura fica só como texto
                }
            });
        }
        return c;
    }

    private PdfPCell celulaRotulo(String texto, boolean comLinha) {
        PdfPCell c = new PdfPCell(new Phrase(comLinha ? texto : " ", fonte(10, Font.NORMAL, Color.BLACK)));
        c.setBorder(comLinha ? Rectangle.TOP : Rectangle.NO_BORDER);
        c.setBorderWidth(0.8f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingTop(2);
        return c;
    }

    private PdfPCell celulaDetalhe(Assinatura a, OrdemServico os) {
        Phrase f = new Phrase();
        if (a != null) {
            f.add(new Chunk(a.getNome() + " · " + Formatos.dataHora(a.getAssinadoEm()), fonte(7, Font.NORMAL, CINZA)));
            if (os != null && !assinaturas.valida(a, os)) {
                f.add(new Chunk("\ndocumento alterado após a assinatura", fonte(7, Font.BOLD, new Color(176, 0, 32))));
            }
        } else {
            f.add(new Chunk(" ", fonte(7, Font.NORMAL, CINZA)));
        }
        PdfPCell c = semBorda(new PdfPCell(f));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }

    private PdfPCell linhaCompleta(String texto, Font fonte, float espacoAntes) {
        PdfPCell c = semBorda(new PdfPCell(new Phrase(texto, fonte)));
        c.setColspan(3);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingTop(espacoAntes);
        return c;
    }

    private static PdfPCell semBorda(PdfPCell c) {
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    /**
     * Coloca as assinaturas e, abaixo delas, o texto de rodapé encostado no pé da página. Se o conteúdo já
     * ocupa o espaço, tudo passa para uma nova página.
     */
    private void posicionarNoRodape(Document doc, PdfWriter writer, PdfPTable bloco, String textoRodape)
            throws DocumentException, IOException {
        BaseFont base = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        float larguraUtil = doc.right() - doc.left();
        List<String> linhas = Quebra.linhas(textoRodape, larguraUtil - 12,
                s -> base.getWidthPoint(s, TAMANHO_RODAPE), MAX_LINHAS_RODAPE);
        float alturaTexto = linhas.isEmpty() ? 0 : linhas.size() * ENTRELINHA_RODAPE + 12;

        float inferior = doc.bottom();
        if (writer.getVerticalPosition(false) < inferior + alturaTexto + RESERVA_ASSINATURAS + 10) {
            doc.newPage();
        }
        ColumnText coluna = new ColumnText(writer.getDirectContent());
        coluna.setSimpleColumn(doc.left(), inferior + alturaTexto, doc.right(), inferior + alturaTexto + RESERVA_ASSINATURAS);
        coluna.addElement(bloco);
        coluna.go();

        if (!linhas.isEmpty()) {
            desenharRodape(writer.getDirectContent(), base, linhas, doc.left(), doc.right(), inferior);
        }
    }

    /** Texto centralizado, cinza e pequeno, com um fio fino acima; a última linha fica rente à margem inferior. */
    private void desenharRodape(PdfContentByte cb, BaseFont base, List<String> linhas, float esquerda, float direita,
            float inferior) {
        float centro = (esquerda + direita) / 2;
        float yUltima = inferior + 4;
        float yFio = yUltima + (linhas.size() - 1) * ENTRELINHA_RODAPE + ENTRELINHA_RODAPE;

        cb.saveState();
        cb.setLineWidth(0.5f);
        cb.setColorStroke(new Color(160, 160, 160));
        cb.moveTo(esquerda, yFio);
        cb.lineTo(direita, yFio);
        cb.stroke();

        cb.beginText();
        cb.setFontAndSize(base, TAMANHO_RODAPE);
        cb.setColorFill(CINZA);
        for (int i = 0; i < linhas.size(); i++) {
            float y = yUltima + (linhas.size() - 1 - i) * ENTRELINHA_RODAPE;
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, linhas.get(i), centro, y, 0);
        }
        cb.endText();
        cb.restoreState();
    }

    private static Font fonte(float tamanho, int estilo, Color cor) {
        return FontFactory.getFont(FontFactory.HELVETICA, tamanho, estilo, cor);
    }
}
