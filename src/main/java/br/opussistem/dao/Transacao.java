package br.opussistem.dao;

import br.opussistem.exception.AppException;
import br.opussistem.exception.DaoException;
import br.opussistem.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

/** Executa trabalho JDBC com conexão aberta e fechada automaticamente. */
public final class Transacao {

    private Transacao() {
    }

    /** Várias operações na mesma transação (commit no fim, rollback em erro). */
    public static <T> T executar(TrabalhoSql<T> trabalho) {
        try (Connection c = DatabaseConnection.conectar()) {
            c.setAutoCommit(false);
            try {
                T resultado = trabalho.executar(c);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Erro no banco de dados: " + e.getMessage(), e);
        }
    }

    /** Consulta simples, sem transação. */
    public static <T> T leitura(TrabalhoSql<T> trabalho) {
        try (Connection c = DatabaseConnection.conectar()) {
            return trabalho.executar(c);
        } catch (SQLException e) {
            throw new DaoException("Erro no banco de dados: " + e.getMessage(), e);
        } catch (AppException e) {
            throw e;
        }
    }
}
