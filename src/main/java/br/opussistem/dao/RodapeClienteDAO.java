package br.opussistem.dao;

import br.opussistem.model.RodapeCliente;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rodapés por cliente. A chave é o nome do cliente; a collation do banco (utf8mb4_0900_ai_ci)
 * ignora maiúsculas/minúsculas e acentos, então "João" e "joao" são o mesmo cliente.
 */
public class RodapeClienteDAO {

    public Optional<String> buscar(String cliente) {
        return Transacao.leitura(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT texto FROM rodapes_cliente WHERE cliente = ?")) {
                ps.setString(1, cliente);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.<String>empty();
                }
            }
        });
    }

    public List<RodapeCliente> listar() {
        return Transacao.leitura(c -> {
            List<RodapeCliente> lista = new ArrayList<>();
            try (Statement st = c.createStatement();
                    ResultSet rs = st.executeQuery("SELECT cliente, texto FROM rodapes_cliente ORDER BY cliente")) {
                while (rs.next()) {
                    lista.add(new RodapeCliente(rs.getString(1), rs.getString(2)));
                }
            }
            return lista;
        });
    }

    public void salvar(String cliente, String texto) {
        Transacao.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO rodapes_cliente (cliente, texto) VALUES (?, ?) "
                            + "ON DUPLICATE KEY UPDATE texto = VALUES(texto)")) {
                ps.setString(1, cliente);
                ps.setString(2, texto);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void remover(String cliente) {
        Transacao.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM rodapes_cliente WHERE cliente = ?")) {
                ps.setString(1, cliente);
                ps.executeUpdate();
            }
            return null;
        });
    }
}
