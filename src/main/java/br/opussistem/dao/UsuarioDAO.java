package br.opussistem.dao;

import br.opussistem.model.PerfilUsuario;
import br.opussistem.model.Usuario;
import br.opussistem.util.Sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioDAO {

    private static final String SELECT = "SELECT id, nome, usuario, email, cargo, perfil, senha_hash, "
            + "assinatura_arquivo, ativo, criado_em FROM usuarios";

    public long inserir(Usuario u) {
        return Transacao.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO usuarios (nome, usuario, email, cargo, perfil, senha_hash, ativo) VALUES (?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, u.getNome());
                ps.setString(2, u.getUsuario());
                Sql.setTexto(ps, 3, u.getEmail());
                Sql.setTexto(ps, 4, u.getCargo());
                ps.setString(5, u.getPerfil().name());
                ps.setString(6, u.getSenhaHash());
                ps.setBoolean(7, u.isAtivo());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    public Optional<Usuario> buscarPorUsuario(String usuario) {
        return Transacao.leitura(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE usuario = ?")) {
                ps.setString(1, usuario);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public Optional<Usuario> buscarPorId(long id) {
        return Transacao.leitura(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE id = ?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    public List<Usuario> listar() {
        return Transacao.leitura(c -> {
            List<Usuario> lista = new ArrayList<>();
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(SELECT + " ORDER BY nome")) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        });
    }

    public int contar() {
        return Transacao.leitura(c -> {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM usuarios")) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }

    public void atualizarDados(Usuario u) {
        Transacao.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE usuarios SET nome = ?, email = ?, cargo = ?, perfil = ?, ativo = ? WHERE id = ?")) {
                ps.setString(1, u.getNome());
                Sql.setTexto(ps, 2, u.getEmail());
                Sql.setTexto(ps, 3, u.getCargo());
                ps.setString(4, u.getPerfil().name());
                ps.setBoolean(5, u.isAtivo());
                ps.setLong(6, u.getId());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void atualizarSenha(long id, String hash) {
        Transacao.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE usuarios SET senha_hash = ? WHERE id = ?")) {
                ps.setString(1, hash);
                ps.setLong(2, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void atualizarAssinatura(long id, String arquivo) {
        Transacao.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE usuarios SET assinatura_arquivo = ? WHERE id = ?")) {
                Sql.setTexto(ps, 1, arquivo);
                ps.setLong(2, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void excluir(long id) {
        Transacao.executar(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM usuarios WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getLong("id"));
        u.setNome(rs.getString("nome"));
        u.setUsuario(rs.getString("usuario"));
        u.setEmail(rs.getString("email"));
        u.setCargo(rs.getString("cargo"));
        u.setPerfil(PerfilUsuario.valueOf(rs.getString("perfil")));
        u.setSenhaHash(rs.getString("senha_hash"));
        u.setAssinaturaArquivo(rs.getString("assinatura_arquivo"));
        u.setAtivo(rs.getBoolean("ativo"));
        u.setCriadoEm(Sql.timestamp(rs, "criado_em"));
        return u;
    }
}
