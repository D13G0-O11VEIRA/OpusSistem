package br.opussistem.service;

import br.opussistem.util.Html;
import com.lowagie.text.Chunk;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.ListItem;
import com.lowagie.text.Paragraph;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converte o HTML do editor de texto formatado (negrito, itálico, sublinhado, tachado, cor,
 * listas, alinhamento) em elementos do PDF (OpenPDF).
 */
final class ConversorHtmlPdf {

    private static final Pattern HEX = Pattern.compile("#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})");
    private static final Pattern RGB = Pattern.compile("rgb\\((\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\)");
    private static final float[] ESCALA_FONT_SIZE = {0.7f, 0.8f, 1f, 1.2f, 1.5f, 2f, 2.8f};

    private record Estilo(boolean negrito, boolean italico, boolean sublinhado, boolean tachado, Color cor, float tamanho) {

        Estilo comNegrito() {
            return new Estilo(true, italico, sublinhado, tachado, cor, tamanho);
        }

        Font fonte() {
            int estilo = Font.NORMAL;
            if (negrito) {
                estilo |= Font.BOLD;
            }
            if (italico) {
                estilo |= Font.ITALIC;
            }
            if (sublinhado) {
                estilo |= Font.UNDERLINE;
            }
            if (tachado) {
                estilo |= Font.STRIKETHRU;
            }
            return FontFactory.getFont(FontFactory.HELVETICA, tamanho, estilo, cor);
        }
    }

    private static final class Contexto {
        final List<com.lowagie.text.Element> saida = new ArrayList<>();
        Paragraph corrente;
        int alinhamento = com.lowagie.text.Element.ALIGN_LEFT;
        final float tamanhoBase;

        Contexto(float tamanhoBase) {
            this.tamanhoBase = tamanhoBase;
        }

        void encerrarParagrafo() {
            if (corrente != null) {
                if (!corrente.isEmpty()) {
                    corrente.setAlignment(alinhamento);
                    saida.add(corrente);
                }
                corrente = null;
            }
        }

        Paragraph paragrafo() {
            if (corrente == null) {
                corrente = new Paragraph();
                corrente.setLeading(tamanhoBase * 1.3f);
            }
            return corrente;
        }
    }

    private ConversorHtmlPdf() {
    }

    static List<com.lowagie.text.Element> converter(String html, float tamanhoBase) {
        Contexto ctx = new Contexto(tamanhoBase);
        Estilo base = new Estilo(false, false, false, false, Color.BLACK, tamanhoBase);
        if (html == null || html.isBlank()) {
            return List.of(new Paragraph(" ", base.fonte()));
        }
        if (!Html.pareceHtml(html)) {
            for (String linha : html.split("\\R")) {
                ctx.saida.add(new Paragraph(linha.isEmpty() ? " " : linha, base.fonte()));
            }
            return ctx.saida;
        }
        Element corpo = Jsoup.parseBodyFragment(Html.corpo(html)).body();
        percorrer(corpo.childNodes(), ctx, base);
        ctx.encerrarParagrafo();
        if (ctx.saida.isEmpty()) {
            ctx.saida.add(new Paragraph(" ", base.fonte()));
        }
        return ctx.saida;
    }

    private static void percorrer(List<Node> nos, Contexto ctx, Estilo estilo) {
        for (Node no : nos) {
            if (no instanceof TextNode texto) {
                String t = texto.text().replaceAll("\\s+", " ");
                if (!t.isBlank() || ctx.corrente != null) {
                    if (ctx.corrente == null) {
                        t = t.stripLeading();
                    }
                    if (!t.isEmpty()) {
                        ctx.paragrafo().add(new Chunk(t, estilo.fonte()));
                    }
                }
            } else if (no instanceof Element el) {
                tratarElemento(el, ctx, estilo);
            }
        }
    }

    private static void tratarElemento(Element el, Contexto ctx, Estilo estilo) {
        String tag = el.tagName().toLowerCase();
        switch (tag) {
            case "br" -> ctx.paragrafo().add(Chunk.NEWLINE);
            case "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote" -> {
                ctx.encerrarParagrafo();
                int alinhamentoAnterior = ctx.alinhamento;
                ctx.alinhamento = alinhamento(el, alinhamentoAnterior);
                Estilo novo = tag.startsWith("h") && tag.length() == 2 ? estiloDe(el, estilo).comNegrito() : estiloDe(el, estilo);
                percorrer(el.childNodes(), ctx, novo);
                ctx.encerrarParagrafo();
                ctx.alinhamento = alinhamentoAnterior;
            }
            case "ul", "ol" -> {
                ctx.encerrarParagrafo();
                com.lowagie.text.List lista = new com.lowagie.text.List(tag.equals("ol"), 14);
                if (tag.equals("ul")) {
                    lista.setListSymbol(new Chunk("\u2022 ", estilo.fonte()));
                }
                for (Element li : el.children()) {
                    if (li.tagName().equalsIgnoreCase("li")) {
                        Contexto interno = new Contexto(ctx.tamanhoBase);
                        percorrer(li.childNodes(), interno, estiloDe(li, estilo));
                        interno.encerrarParagrafo();
                        ListItem item = new ListItem();
                        item.setLeading(ctx.tamanhoBase * 1.3f);
                        for (com.lowagie.text.Element e : interno.saida) {
                            if (e instanceof Paragraph p) {
                                for (Object chunk : p.getChunks()) {
                                    item.add((Chunk) chunk);
                                }
                            }
                        }
                        lista.add(item);
                    }
                }
                ctx.saida.add(lista);
            }
            default -> percorrer(el.childNodes(), ctx, estiloDe(el, estilo));
        }
    }

    /** Aplica o efeito das tags de formatação e do atributo style/color/size. */
    private static Estilo estiloDe(Element el, Estilo e) {
        String tag = el.tagName().toLowerCase();
        boolean negrito = e.negrito() || tag.equals("b") || tag.equals("strong");
        boolean italico = e.italico() || tag.equals("i") || tag.equals("em");
        boolean sublinhado = e.sublinhado() || tag.equals("u");
        boolean tachado = e.tachado() || tag.equals("s") || tag.equals("strike") || tag.equals("del");
        Color cor = e.cor();
        float tamanho = e.tamanho();

        if (tag.equals("font")) {
            Color c = cor(el.attr("color"));
            if (c != null) {
                cor = c;
            }
            tamanho = tamanhoFont(el.attr("size"), tamanho);
        }
        for (String decl : el.attr("style").split(";")) {
            String[] kv = decl.split(":", 2);
            if (kv.length < 2) {
                continue;
            }
            String chave = kv[0].trim().toLowerCase();
            String valor = kv[1].trim().toLowerCase();
            switch (chave) {
                case "font-weight" -> negrito = negrito || valor.equals("bold") || valor.equals("700") || valor.equals("800") || valor.equals("900");
                case "font-style" -> italico = italico || valor.equals("italic");
                case "text-decoration", "text-decoration-line" -> {
                    sublinhado = sublinhado || valor.contains("underline");
                    tachado = tachado || valor.contains("line-through");
                }
                case "color" -> {
                    Color c = cor(valor);
                    if (c != null) {
                        cor = c;
                    }
                }
                case "font-size" -> {
                    Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(px|pt)").matcher(valor);
                    if (m.find()) {
                        float px = Float.parseFloat(m.group(1));
                        tamanho = Math.max(6f, Math.min(28f, px * 0.75f));
                    }
                }
                default -> { }
            }
        }
        return new Estilo(negrito, italico, sublinhado, tachado, cor, tamanho);
    }

    private static float tamanhoFont(String atributo, float atual) {
        try {
            int n = Integer.parseInt(atributo.trim());
            if (n >= 1 && n <= 7) {
                return atual * ESCALA_FONT_SIZE[n - 1];
            }
        } catch (NumberFormatException ignorado) {
            // atributo vazio ou inválido
        }
        return atual;
    }

    private static int alinhamento(Element el, int atual) {
        String valor = el.attr("align").toLowerCase();
        for (String decl : el.attr("style").split(";")) {
            String[] kv = decl.split(":", 2);
            if (kv.length == 2 && kv[0].trim().equalsIgnoreCase("text-align")) {
                valor = kv[1].trim().toLowerCase();
            }
        }
        return switch (valor) {
            case "center" -> com.lowagie.text.Element.ALIGN_CENTER;
            case "right" -> com.lowagie.text.Element.ALIGN_RIGHT;
            case "justify" -> com.lowagie.text.Element.ALIGN_JUSTIFIED;
            case "left" -> com.lowagie.text.Element.ALIGN_LEFT;
            default -> atual;
        };
    }

    private static Color cor(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String t = texto.trim();
        Matcher h = HEX.matcher(t);
        if (h.find()) {
            String hex = h.group(1);
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
            }
            return new Color(Integer.parseInt(hex, 16));
        }
        Matcher r = RGB.matcher(t);
        if (r.find()) {
            return new Color(Math.min(255, Integer.parseInt(r.group(1))), Math.min(255, Integer.parseInt(r.group(2))),
                    Math.min(255, Integer.parseInt(r.group(3))));
        }
        return switch (t.toLowerCase()) {
            case "red" -> Color.RED;
            case "blue" -> Color.BLUE;
            case "green" -> new Color(0, 128, 0);
            case "black" -> Color.BLACK;
            case "gray", "grey" -> Color.GRAY;
            case "orange" -> Color.ORANGE;
            default -> null;
        };
    }
}
