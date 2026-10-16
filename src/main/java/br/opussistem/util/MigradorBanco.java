package br.opussistem.util;

import br.opussistem.config.AppConfig;
import br.opussistem.exception.AppException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cria o banco (se não existir) e aplica as migrações de database/migrations
 * que ainda não foram aplicadas. A lista fica em db/migration/migrations.txt.
 */
public final class MigradorBanco {

    private static final System.Logger LOG = System.getLogger(MigradorBanco.class.getName());
    /** 1050 tabela existe, 1060 coluna duplicada, 1061 índice duplicado, 1091 item inexistente. */
    private static final Set<Integer> JA_APLICADO = Set.of(1050, 1060, 1061, 1091);

    private MigradorBanco() {
    }

    public static void migrar() {
        try (Connection c = DatabaseConnection.conectarSemBanco(); Statement st = c.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS `" + AppConfig.nomeBanco()
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        } catch (SQLException e) {
            throw new AppException("Não foi possível conectar ao MySQL: " + e.getMessage()
                    + "\nConfira config/application.properties e se o servidor está ligado.", e);
        }

        try (Connection c = DatabaseConnection.conectar()) {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS schema_migrations ("
                        + "versao VARCHAR(100) PRIMARY KEY, "
                        + "aplicada_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            }
            Set<String> aplicadas = new HashSet<>();
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT versao FROM schema_migrations")) {
                while (rs.next()) {
                    aplicadas.add(rs.getString(1));
                }
            }
            for (String arquivo : listarMigracoes()) {
                if (!aplicadas.contains(arquivo)) {
                    aplicar(c, arquivo);
                }
            }
        } catch (SQLException e) {
            throw new AppException("Erro ao preparar o banco de dados: " + e.getMessage(), e);
        }
    }

    private static List<String> listarMigracoes() {
        List<String> nomes = new ArrayList<>();
        for (String linha : lerRecurso("db/migration/migrations.txt").split("\\R")) {
            if (!linha.isBlank() && !linha.startsWith("#")) {
                nomes.add(linha.trim());
            }
        }
        return nomes;
    }

    private static void aplicar(Connection c, String arquivo) throws SQLException {
        LOG.log(System.Logger.Level.INFO, "Aplicando migração {0}", arquivo);
        for (String comando : dividir(lerRecurso("db/migration/" + arquivo))) {
            try (Statement st = c.createStatement()) {
                st.execute(comando);
            } catch (SQLException e) {
                if (!JA_APLICADO.contains(e.getErrorCode())) {
                    throw e;
                }
                LOG.log(System.Logger.Level.INFO, "Já aplicado, ignorando: {0}", e.getMessage());
            }
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO schema_migrations (versao) VALUES (?)")) {
            ps.setString(1, arquivo);
            ps.executeUpdate();
        }
    }

    /** Separa o script em comandos (ignora linhas de comentário "--"). */
    static List<String> dividir(String script) {
        StringBuilder limpo = new StringBuilder();
        for (String linha : script.split("\\R")) {
            if (!linha.trim().startsWith("--")) {
                limpo.append(linha).append('\n');
            }
        }
        List<String> comandos = new ArrayList<>();
        for (String parte : limpo.toString().split(";\\s*(\\R|$)")) {
            if (!parte.isBlank()) {
                comandos.add(parte.trim());
            }
        }
        return comandos;
    }

    private static String lerRecurso(String caminho) {
        try (InputStream in = MigradorBanco.class.getClassLoader().getResourceAsStream(caminho)) {
            if (in == null) {
                throw new AppException("Recurso não encontrado: " + caminho);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AppException("Erro ao ler " + caminho, e);
        }
    }
}
