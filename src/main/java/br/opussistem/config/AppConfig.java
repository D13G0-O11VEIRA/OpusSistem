package br.opussistem.config;

import br.opussistem.exception.AppException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Lê config/application.properties (ou o caminho em -Dopus.config / OPUS_CONFIG).
 * Qualquer chave pode ser sobrescrita por variável de ambiente:
 * db.password -> OPUS_DB_PASSWORD.
 */
public final class AppConfig {

    private static final Properties PROPS = new Properties();
    private static boolean carregado;
    private static boolean arquivoEncontrado;

    private AppConfig() {
    }

    public static synchronized void carregar() {
        if (carregado) {
            return;
        }
        String caminho = System.getProperty("opus.config",
                System.getenv().getOrDefault("OPUS_CONFIG", "config/application.properties"));
        Path arquivo = Path.of(caminho);
        if (Files.exists(arquivo)) {
            try (Reader leitor = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
                PROPS.load(leitor);
                arquivoEncontrado = true;
            } catch (IOException e) {
                throw new AppException("Não foi possível ler " + arquivo + ": " + e.getMessage(), e);
            }
        }
        carregado = true;
    }

    public static String get(String chave, String padrao) {
        carregar();
        String env = System.getenv("OPUS_" + chave.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) {
            return env;
        }
        String valor = PROPS.getProperty(chave);
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }

    /** Há configuração de banco (arquivo ou variáveis de ambiente)? */
    public static boolean bancoConfigurado() {
        carregar();
        return arquivoEncontrado || System.getenv("OPUS_DB_USER") != null;
    }

    public static String urlJdbc(boolean comBanco) {
        String host = get("db.host", "localhost");
        String porta = get("db.port", "3306");
        String banco = comBanco ? get("db.name", "opussistem") : "";
        String fuso = get("db.timezone", "America/Fortaleza");
        String extras = get("db.params",
                "useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&connectTimeout=5000");
        return "jdbc:mysql://" + host + ":" + porta + "/" + banco + "?serverTimezone=" + fuso
                + (extras.isBlank() ? "" : "&" + extras);
    }

    public static String nomeBanco() {
        return get("db.name", "opussistem");
    }

    public static String usuarioBanco() {
        return get("db.user", "root");
    }

    public static String senhaBanco() {
        return get("db.password", "");
    }

    public static Path pastaStorage() {
        return Path.of(get("storage.dir", "storage"));
    }
}
