package br.opussistem.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Contador anual usado na numeração das OS (OS-2026-0001). */
public class SequenciaDAO {

    /** Deve ser chamado dentro de uma transação: trava a linha até o commit. */
    public int proximo(Connection c, String nome, int ano) throws SQLException {
        int proximo;
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT ultimo FROM sequencias WHERE nome = ? AND ano = ? FOR UPDATE")) {
            ps.setString(1, nome);
            ps.setInt(2, ano);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    proximo = rs.getInt(1) + 1;
                    try (PreparedStatement up = c.prepareStatement(
                            "UPDATE sequencias SET ultimo = ? WHERE nome = ? AND ano = ?")) {
                        up.setInt(1, proximo);
                        up.setString(2, nome);
                        up.setInt(3, ano);
                        up.executeUpdate();
                    }
                    return proximo;
                }
            }
        }
        proximo = 1;
        try (PreparedStatement ins = c.prepareStatement("INSERT INTO sequencias (nome, ano, ultimo) VALUES (?, ?, 1)")) {
            ins.setString(1, nome);
            ins.setInt(2, ano);
            ins.executeUpdate();
        }
        return proximo;
    }
}
