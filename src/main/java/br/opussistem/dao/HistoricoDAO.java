package br.opussistem.dao;

import br.opussistem.model.CategoriaEvento;
import br.opussistem.model.EventoHistorico;
import br.opussistem.util.Sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HistoricoDAO {

    public void inserir(Connection c, EventoHistorico e) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO historico_os (os_id, data_hora, categoria, titulo, descricao, responsavel) VALUES (?,?,?,?,?,?)")) {
            ps.setLong(1, e.getOsId());
            Sql.setTimestamp(ps, 2, e.getDataHora());
            ps.setString(3, e.getCategoria().name());
            ps.setString(4, e.getTitulo());
            Sql.setTexto(ps, 5, e.getDescricao());
            Sql.setTexto(ps, 6, e.getResponsavel());
            ps.executeUpdate();
        }
    }

    public List<EventoHistorico> listarPorOS(long osId) {
        return Transacao.leitura(c -> consultar(c,
                "SELECT h.*, o.numero_os FROM historico_os h JOIN ordens_servico o ON o.id = h.os_id "
                        + "WHERE h.os_id = ? ORDER BY h.data_hora, h.id", osId));
    }

    public List<EventoHistorico> recentes(int n) {
        return Transacao.leitura(c -> consultar(c,
                "SELECT h.*, o.numero_os FROM historico_os h JOIN ordens_servico o ON o.id = h.os_id "
                        + "ORDER BY h.data_hora DESC, h.id DESC LIMIT ?", n));
    }

    private List<EventoHistorico> consultar(Connection c, String sql, long parametro) throws SQLException {
        List<EventoHistorico> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EventoHistorico e = new EventoHistorico();
                    e.setId(rs.getLong("id"));
                    e.setOsId(rs.getLong("os_id"));
                    e.setNumeroOs(rs.getString("numero_os"));
                    e.setDataHora(Sql.timestamp(rs, "data_hora"));
                    e.setCategoria(CategoriaEvento.valueOf(rs.getString("categoria")));
                    e.setTitulo(rs.getString("titulo"));
                    e.setDescricao(rs.getString("descricao"));
                    e.setResponsavel(rs.getString("responsavel"));
                    lista.add(e);
                }
            }
        }
        return lista;
    }
}
