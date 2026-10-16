package br.opussistem.util;

import br.opussistem.config.AppConfig;
import br.opussistem.exception.AppException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Abre conexões JDBC com o MySQL. Usuário, senha e servidor vêm de
 * config/application.properties (nunca ficam no código-fonte).
 */
public class DatabaseConnection {

    public static Connection conectar() throws SQLException {
        verificarConfiguracao();
        return DriverManager.getConnection(AppConfig.urlJdbc(true),
                AppConfig.usuarioBanco(), AppConfig.senhaBanco());
    }

    /** Conexão ao servidor sem escolher o banco (usada para criar o banco na primeira execução). */
    public static Connection conectarSemBanco() throws SQLException {
        verificarConfiguracao();
        return DriverManager.getConnection(AppConfig.urlJdbc(false),
                AppConfig.usuarioBanco(), AppConfig.senhaBanco());
    }

    private static void verificarConfiguracao() {
        if (!AppConfig.bancoConfigurado()) {
            throw new AppException("Arquivo config/application.properties não encontrado.\n"
                    + "Copie config/application.example.properties para config/application.properties "
                    + "e preencha o usuário e a senha do MySQL.");
        }
    }
}
