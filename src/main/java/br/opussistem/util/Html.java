package br.opussistem.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utilidades para o HTML gerado pelo editor de texto formatado (sem dependências externas). */
public final class Html {

    private static final Pattern CORPO = Pattern.compile("<body[^>]*>(.*)</body>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAG = Pattern.compile("<[a-zA-Z/!][^>]*>");
    private static final Pattern ENTIDADE_NUM = Pattern.compile("&#(\\d+);");

    private Html() {
    }

    public static boolean pareceHtml(String s) {
        return s != null && TAG.matcher(s).find();
    }

    /** Extrai o conteúdo do corpo (o editor devolve um documento completo). */
    public static String corpo(String html) {
        if (html == null) {
            return "";
        }
        Matcher m = CORPO.matcher(html);
        String c = m.find() ? m.group(1) : html;
        return c.replaceAll("\\s+contenteditable=\"[^\"]*\"", "").trim();
    }

    /** Converte HTML em texto simples (quebras de linha preservadas). */
    public static String paraTexto(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        if (!pareceHtml(html)) {
            return html.trim();
        }
        String s = corpo(html)
                .replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", "")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|li|h[1-6]|tr)>", "\n")
                .replaceAll("(?i)<li[^>]*>", "• ")
                .replaceAll("<[^>]+>", "");
        s = s.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
        Matcher m = ENTIDADE_NUM.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(m.group(1)))));
        }
        m.appendTail(sb);
        return sb.toString().replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").trim();
    }

    public static boolean vazio(String html) {
        return paraTexto(html).isBlank();
    }

    /** Texto simples -> HTML (um parágrafo por linha). */
    public static String deTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        if (pareceHtml(texto)) {
            return texto;
        }
        StringBuilder sb = new StringBuilder();
        for (String linha : texto.split("\\R")) {
            sb.append("<p>").append(escapar(linha)).append("</p>");
        }
        return sb.toString();
    }

    public static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
