package br.opussistem.util;

import br.opussistem.config.AppConfig;
import br.opussistem.exception.AppException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Pastas de arquivos do sistema (assinaturas, relatórios, anexos, logo). */
public final class Armazenamento {

    private Armazenamento() {
    }

    public static Path raiz() {
        return AppConfig.pastaStorage();
    }

    public static Path assinaturas() {
        return garantir(raiz().resolve("assinaturas"));
    }

    public static Path relatorios() {
        return garantir(raiz().resolve("relatorios"));
    }

    public static Path anexos(String numeroOs) {
        return garantir(raiz().resolve("ordens").resolve(nomeSeguro(numeroOs)));
    }

    public static Path garantir(Path pasta) {
        try {
            Files.createDirectories(pasta);
            return pasta;
        } catch (IOException e) {
            throw new AppException("Não foi possível criar a pasta " + pasta + ": " + e.getMessage(), e);
        }
    }

    public static Path gravar(Path destino, byte[] conteudo) {
        try {
            garantir(destino.getParent());
            Files.write(destino, conteudo);
            return destino;
        } catch (IOException e) {
            throw new AppException("Não foi possível gravar " + destino + ": " + e.getMessage(), e);
        }
    }

    public static Path copiar(Path origem, Path destino) {
        try {
            garantir(destino.getParent());
            Files.copy(origem, destino, StandardCopyOption.REPLACE_EXISTING);
            return destino;
        } catch (IOException e) {
            throw new AppException("Não foi possível copiar o arquivo: " + e.getMessage(), e);
        }
    }

    public static byte[] ler(Path arquivo) {
        try {
            return Files.readAllBytes(arquivo);
        } catch (IOException e) {
            throw new AppException("Não foi possível ler " + arquivo + ": " + e.getMessage(), e);
        }
    }

    public static void apagarSilencioso(Path arquivo) {
        try {
            if (arquivo != null) {
                Files.deleteIfExists(arquivo);
            }
        } catch (IOException ignorado) {
            // arquivo já inexistente ou em uso: não é crítico
        }
    }

    public static String nomeSeguro(String nome) {
        return nome == null ? "sem-nome" : nome.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
