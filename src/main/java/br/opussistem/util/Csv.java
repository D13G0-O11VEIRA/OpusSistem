package br.opussistem.util;

import java.util.ArrayList;
import java.util.List;

/** Leitura e escrita de CSV (aceita ; , tab ou | e campos entre aspas). */
public final class Csv {

    private Csv() {
    }

    public static List<String[]> ler(String texto) {
        String t = texto.startsWith("\uFEFF") ? texto.substring(1) : texto;
        char sep = detectarSeparador(t);
        List<String[]> linhas = new ArrayList<>();
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean aspas = false;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (aspas) {
                if (ch == '"') {
                    if (i + 1 < t.length() && t.charAt(i + 1) == '"') {
                        atual.append('"');
                        i++;
                    } else {
                        aspas = false;
                    }
                } else {
                    atual.append(ch);
                }
            } else if (ch == '"') {
                aspas = true;
            } else if (ch == sep) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < t.length() && t.charAt(i + 1) == '\n') {
                    i++;
                }
                campos.add(atual.toString());
                atual.setLength(0);
                adicionar(linhas, campos);
                campos = new ArrayList<>();
            } else {
                atual.append(ch);
            }
        }
        if (atual.length() > 0 || !campos.isEmpty()) {
            campos.add(atual.toString());
            adicionar(linhas, campos);
        }
        return linhas;
    }

    private static void adicionar(List<String[]> linhas, List<String> campos) {
        boolean algum = campos.stream().anyMatch(c -> !c.isBlank());
        if (algum) {
            linhas.add(campos.toArray(new String[0]));
        }
    }

    private static char detectarSeparador(String t) {
        int fim = t.indexOf('\n');
        String primeira = fim < 0 ? t : t.substring(0, fim);
        char melhor = ';';
        int max = -1;
        for (char c : new char[]{';', ',', '\t', '|'}) {
            int n = (int) primeira.chars().filter(x -> x == c).count();
            if (n > max) {
                max = n;
                melhor = c;
            }
        }
        return melhor;
    }

    public static String escrever(List<String[]> linhas) {
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM: o Excel reconhece UTF-8
        for (String[] linha : linhas) {
            for (int i = 0; i < linha.length; i++) {
                if (i > 0) {
                    sb.append(';');
                }
                sb.append(escapar(linha[i]));
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    private static String escapar(String v) {
        String s = v == null ? "" : v;
        if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
