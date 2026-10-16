package br.opussistem.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class ConfiguracaoDAO {

    public Map<String, String> todas() {
        return Transacao.leitura(c -> {
            Map<String, String> mapa = new HashMap<>();
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT chave, valor FROM configuracoes")) {
                while (rs.next()) {
                    mapa.put(rs.getString(1), rs.getString(2));
                }
            }
            return mapa;
        });
    }

    public void salvarTodas(Map<String, String> valores) {
        Transacao.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO configuracoes (chave, valor) VALUES (?, ?) ON DUPLICATE KEY UPDATE valor = VALUES(valor)")) {
                for (Map.Entry<String, String> e : valores.entrySet()) {
                    ps.setString(1, e.getKey());
                    ps.setString(2, e.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return null;
        });
    }
}
