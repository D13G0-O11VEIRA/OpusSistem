package br.opussistem.dao;

import br.opussistem.model.Assinatura;
import br.opussistem.model.PacoteAssinatura;
import br.opussistem.model.PapelAssinatura;
import br.opussistem.util.Sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssinaturaDAO {

    private static final String SELECT = "SELECT id, os_id, papel, nome, email, arquivo, codigo_verificacao, "
            + "pacote_codigo, assinado_em FROM assinaturas";

    /** Grava a assinatura, substituindo a anterior do mesmo papel na mesma OS. */
    public void substituir(Connection c, Assinatura a) throws SQLException {
        remover(c, a.getOsId(), a.getPapel());
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO assinaturas (os_id, papel, nome, email, arquivo, codigo_verificacao, pacote_codigo, assinado_em) "
                        + "VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setLong(1, a.getOsId());
            ps.setString(2, a.getPapel().name());
            ps.setString(3, a.getNome());
            Sql.setTexto(ps, 4, a.getEmail());
            ps.setString(5, a.getArquivo());
            ps.setString(6, a.getCodigoVerificacao());
            Sql.setTexto(ps, 7, a.getPacoteCodigo());
            Sql.setTimestamp(ps, 8, a.getAssinadoEm());
            ps.executeUpdate();
        }
    }

    public void remover(Connection c, long osId, PapelAssinatura papel) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM assinaturas WHERE os_id = ? AND papel = ?")) {
            ps.setLong(1, osId);
            ps.setString(2, papel.name());
            ps.executeUpdate();
        }
    }

    public List<Assinatura> listarPorOS(Connection c, long osId) throws SQLException {
        List<Assinatura> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE os_id = ?")) {
            ps.setLong(1, osId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public List<Assinatura> listarPorOS(long osId) {
        return Transacao.leitura(c -> listarPorOS(c, osId));
    }

    public Optional<Assinatura> buscar(long osId, PapelAssinatura papel) {
        return listarPorOS(osId).stream().filter(a -> a.getPapel() == papel).findFirst();
    }

    public void inserirPacote(Connection c, PacoteAssinatura p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO pacotes_assinatura (codigo, cliente, assinante, email, quantidade, assinado_em) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, p.codigo());
            ps.setString(2, p.cliente());
            ps.setString(3, p.assinante());
            Sql.setTexto(ps, 4, p.email());
            ps.setInt(5, p.quantidade());
            Sql.setTimestamp(ps, 6, p.assinadoEm());
            ps.executeUpdate();
        }
    }

    public List<PacoteAssinatura> listarPacotes(int limite) {
        return Transacao.leitura(c -> {
            List<PacoteAssinatura> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT codigo, cliente, assinante, email, quantidade, assinado_em FROM pacotes_assinatura "
                            + "ORDER BY assinado_em DESC LIMIT ?")) {
                ps.setInt(1, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new PacoteAssinatura(rs.getString("codigo"), rs.getString("cliente"),
                                rs.getString("assinante"), rs.getString("email"), rs.getInt("quantidade"),
                                Sql.timestamp(rs, "assinado_em")));
                    }
                }
            }
            return lista;
        });
    }

    /** IDs das OS vinculadas a um pacote. */
    public List<Long> osDoPacote(String codigo) {
        return Transacao.leitura(c -> {
            List<Long> ids = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT os_id FROM assinaturas WHERE pacote_codigo = ? ORDER BY os_id")) {
                ps.setString(1, codigo);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getLong(1));
                    }
                }
            }
            return ids;
        });
    }

    private Assinatura mapear(ResultSet rs) throws SQLException {
        Assinatura a = new Assinatura();
        a.setId(rs.getLong("id"));
        a.setOsId(rs.getLong("os_id"));
        a.setPapel(PapelAssinatura.valueOf(rs.getString("papel")));
        a.setNome(rs.getString("nome"));
        a.setEmail(rs.getString("email"));
        a.setArquivo(rs.getString("arquivo"));
        a.setCodigoVerificacao(rs.getString("codigo_verificacao"));
        a.setPacoteCodigo(rs.getString("pacote_codigo"));
        a.setAssinadoEm(Sql.timestamp(rs, "assinado_em"));
        return a;
    }
}
