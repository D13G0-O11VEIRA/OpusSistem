package br.opussistem.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Quebra de texto em linhas que cabem em uma largura (usada no rodapé do PDF). */
public final class Quebra {

    private Quebra() {
    }

    /**
     * @param texto      texto com ou sem quebras de linha
     * @param larguraMax largura disponível (na mesma unidade devolvida por {@code medir})
     * @param medir      devolve a largura de um trecho de texto na fonte usada
     * @param maxLinhas  limite de linhas; o excesso é cortado e a última linha termina com "…"
     */
    public static List<String> linhas(String texto, double larguraMax, ToDoubleFunction<String> medir, int maxLinhas) {
        List<String> saida = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return saida;
        }
        for (String paragrafo : texto.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            String p = paragrafo.strip();
            if (p.isEmpty()) {
                saida.add("");
                continue;
            }
            StringBuilder linha = new StringBuilder();
            for (String palavra : p.split("\\s+")) {
                String restante = palavra;
                // palavra maior que a linha inteira: corta por caracteres
                while (medir.applyAsDouble(restante) > larguraMax && restante.length() > 1) {
                    int corte = restante.length() - 1;
                    while (corte > 1 && medir.applyAsDouble(restante.substring(0, corte)) > larguraMax) {
                        corte--;
                    }
                    if (linha.length() > 0) {
                        saida.add(linha.toString());
                        linha.setLength(0);
                    }
                    saida.add(restante.substring(0, corte));
                    restante = restante.substring(corte);
                }
                String tentativa = linha.length() == 0 ? restante : linha + " " + restante;
                if (medir.applyAsDouble(tentativa) <= larguraMax) {
                    linha.setLength(0);
                    linha.append(tentativa);
                } else {
                    saida.add(linha.toString());
                    linha.setLength(0);
                    linha.append(restante);
                }
            }
            if (linha.length() > 0) {
                saida.add(linha.toString());
            }
        }
        while (!saida.isEmpty() && saida.get(saida.size() - 1).isEmpty()) {
            saida.remove(saida.size() - 1);
        }
        if (saida.size() > maxLinhas) {
            List<String> corte = new ArrayList<>(saida.subList(0, maxLinhas));
            corte.set(maxLinhas - 1, corte.get(maxLinhas - 1) + "…");
            return corte;
        }
        return saida;
    }
}
