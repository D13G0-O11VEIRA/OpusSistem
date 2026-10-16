package br.opussistem.dao;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface TrabalhoSql<T> {
    T executar(Connection conexao) throws SQLException;
}
