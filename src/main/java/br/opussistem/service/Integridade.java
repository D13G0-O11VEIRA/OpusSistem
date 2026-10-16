package br.opussistem.service;

import br.opussistem.model.OrdemServico;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Código de verificação do conteúdo de uma OS (SHA-256 abreviado). Gravado no momento da
 * assinatura; se o conteúdo mudar depois, o código deixa de conferir e o relatório avisa.
 */
public final class Integridade {

    private Integridade() {
    }

    public static String codigo(OrdemServico os) {
        String conteudo = String.join("|",
                texto(os.getNumero()), texto(os.getCliente()), texto(os.getEquipamento()), texto(os.getModelo()),
                texto(os.getPatrimonio()), texto(os.getNumeroSerie()), texto(os.getTecnico()),
                texto(os.getDataEntrada()), texto(os.getDataEntrega()), texto(os.getValorFinal()),
                texto(os.getDescricaoProblema()), texto(os.getLaudoTecnico()));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(conteudo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(hash, 0, 5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String texto(Object o) {
        return Objects.toString(o, "");
    }
}
