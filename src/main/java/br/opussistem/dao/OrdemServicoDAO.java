package br.opussistem.dao;

import br.opussistem.model.FiltroOS;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.ResumoMensal;
import br.opussistem.model.StatusOS;
import br.opussistem.util.Sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Acesso à tabela ordens_servico. */
public class OrdemServicoDAO {

    /** Colunas editáveis, na mesma ordem usada por bind(). */
    private static final String[] COLUNAS = {
            "cliente", "telefone", "equipamento", "modelo", "fabricante", "numero_patrimonio",
            "numero_serie", "status_equipamento", "tipo_manutencao", "tecnico_responsavel",
            "valor_previsto", "valor_final", "data_entrada", "previsao_entrega", "data_entrega",
            "proxima_preventiva", "status", "descricao_problema", "laudo_tecnico", "observacoes",
            "rodape_relatorio", "assinatura_cliente"
    };

    private static final String SELECT = "SELECT id, numero_os, " + String.join(", ", COLUNAS)
            + ", criado_em, atualizado_em FROM ordens_servico";

    private static final Set<String> COLUNAS_SUGESTAO = Set.of(
            "equipamento", "fabricante", "tecnico_responsavel", "tipo_manutencao", "status_equipamento", "cliente");

    // ---------------------------------------------------------------- escrita

    public long inserir(Connection c, OrdemServico os) throws SQLException {
        String sql = "INSERT INTO ordens_servico (numero_os, " + String.join(", ", COLUNAS) + ") VALUES (?"
                + ", ?".repeat(COLUNAS.length) + ")";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, os.getNumero());
            bind(ps, os, 2);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void atualizar(Connection c, OrdemServico os) throws SQLException {
        StringBuilder set = new StringBuilder();
        for (String col : COLUNAS) {
            set.append(set.length() == 0 ? "" : ", ").append(col).append(" = ?");
        }
        try (PreparedStatement ps = c.prepareStatement("UPDATE ordens_servico SET " + set + " WHERE id = ?")) {
            bind(ps, os, 1);
            ps.setLong(COLUNAS.length + 1, os.getId());
            ps.executeUpdate();
        }
    }

    public void atualizarStatus(Connection c, long id, StatusOS status, java.time.LocalDateTime dataEntrega) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE ordens_servico SET status = ?, data_entrega = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            Sql.setTimestamp(ps, 2, dataEntrega);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public void atualizarAssinaturaCliente(Connection c, long id, String caminho) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE ordens_servico SET assinatura_cliente = ? WHERE id = ?")) {
            Sql.setTexto(ps, 1, caminho);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void atualizarRodape(Connection c, long id, String texto) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE ordens_servico SET rodape_relatorio = ? WHERE id = ?")) {
            Sql.setTexto(ps, 1, texto);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, OrdemServico os, int i) throws SQLException {
        Sql.setTexto(ps, i++, os.getCliente());
        Sql.setTexto(ps, i++, os.getTelefone());
        Sql.setTexto(ps, i++, os.getEquipamento());
        Sql.setTexto(ps, i++, os.getModelo());
        Sql.setTexto(ps, i++, os.getFabricante());
        Sql.setTexto(ps, i++, os.getPatrimonio());
        Sql.setTexto(ps, i++, os.getNumeroSerie());
        Sql.setTexto(ps, i++, os.getStatusEquipamento());
        Sql.setTexto(ps, i++, os.getTipoManutencao());
        Sql.setTexto(ps, i++, os.getTecnico());
        Sql.setDecimal(ps, i++, os.getValorPrevisto());
        Sql.setDecimal(ps, i++, os.getValorFinal());
        Sql.setTimestamp(ps, i++, os.getDataEntrada());
        Sql.setTimestamp(ps, i++, os.getPrevisaoEntrega());
        Sql.setTimestamp(ps, i++, os.getDataEntrega());
        Sql.setData(ps, i++, os.getProximaPreventiva());
        ps.setString(i++, (os.getStatus() == null ? StatusOS.ABERTA : os.getStatus()).name());
        Sql.setTexto(ps, i++, os.getDescricaoProblema());
        Sql.setTexto(ps, i++, os.getLaudoTecnico());
        Sql.setTexto(ps, i++, os.getObservacoes());
        Sql.setTexto(ps, i++, os.getRodapeRelatorio());
        Sql.setTexto(ps, i, os.getAssinaturaCliente());
    }

    // ---------------------------------------------------------------- leitura

    public Optional<OrdemServico> buscarPorId(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public Optional<OrdemServico> buscarPorId(long id) {
        return Transacao.leitura(c -> buscarPorId(c, id));
    }

    public Optional<OrdemServico> buscarPorNumero(Connection c, String numero) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE numero_os = ?")) {
            ps.setString(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<OrdemServico> pesquisar(FiltroOS f, int limite) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        adicionarLike(where, params, "numero_os", f.numero);
        adicionarLike(where, params, "cliente", f.cliente);
        adicionarLike(where, params, "numero_patrimonio", f.patrimonio);
        adicionarLike(where, params, "numero_serie", f.serie);
        if (preenchido(f.fabricante)) {
            where.append(" AND fabricante = ?");
            params.add(f.fabricante);
        }
        if (preenchido(f.tecnico)) {
            where.append(" AND tecnico_responsavel = ?");
            params.add(f.tecnico);
        }
        if (f.status != null) {
            where.append(" AND status = ?");
            params.add(f.status.name());
        }
        if (f.somenteFechadas) {
            where.append(" AND status = 'FECHADA'");
        }
        if (f.dataInicial != null) {
            where.append(" AND data_entrada >= ?");
            params.add(java.sql.Timestamp.valueOf(f.dataInicial.atStartOfDay()));
        }
        if (f.dataFinal != null) {
            where.append(" AND data_entrada < ?");
            params.add(java.sql.Timestamp.valueOf(f.dataFinal.plusDays(1).atStartOfDay()));
        }
        String sql = SELECT + where + " ORDER BY data_entrada DESC, id DESC LIMIT " + Math.max(1, limite);
        return Transacao.leitura(c -> consultar(c, sql, params));
    }

    public List<OrdemServico> listarRecentes(int n) {
        return pesquisar(new FiltroOS(), n);
    }

    /** Todas as OS (usado no cálculo de alertas de preventiva). */
    public List<OrdemServico> listarParaAlertas() {
        return Transacao.leitura(c -> consultar(c, SELECT + " ORDER BY data_entrada DESC, id DESC LIMIT 50000", List.of()));
    }

    /** Número da OS em aberto que usa o mesmo nº de série, se existir. */
    public Optional<String> existeAbertaComSerie(Connection c, String serie, long excluirId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT numero_os FROM ordens_servico WHERE numero_serie = ? AND status <> 'FECHADA' AND id <> ? LIMIT 1")) {
            ps.setString(1, serie);
            ps.setLong(2, excluirId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }

    public Map<StatusOS, Integer> contarPorStatus() {
        return Transacao.leitura(c -> {
            Map<StatusOS, Integer> mapa = new EnumMap<>(StatusOS.class);
            for (StatusOS s : StatusOS.values()) {
                mapa.put(s, 0);
            }
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT status, COUNT(*) FROM ordens_servico GROUP BY status")) {
                while (rs.next()) {
                    mapa.merge(StatusOS.deTexto(rs.getString(1)), rs.getInt(2), Integer::sum);
                }
            }
            return mapa;
        });
    }

    public int contarFechadasSemAssinaturaCliente() {
        return Transacao.leitura(c -> {
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ordens_servico o WHERE o.status = 'FECHADA' "
                         + "AND NOT EXISTS (SELECT 1 FROM assinaturas a WHERE a.os_id = o.id AND a.papel = 'CLIENTE')")) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }

    public List<ResumoMensal> resumoPorMes(LocalDate desde) {
        return Transacao.leitura(c -> {
            List<ResumoMensal> lista = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT DATE_FORMAT(data_entrada, '%Y-%m') AS ym, "
                            + "SUM(status <> 'FECHADA') AS abertas, SUM(status = 'FECHADA') AS fechadas "
                            + "FROM ordens_servico WHERE data_entrada >= ? GROUP BY ym ORDER BY ym")) {
                Sql.setData(ps, 1, desde);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new ResumoMensal(YearMonth.parse(rs.getString("ym")), rs.getInt("abertas"), rs.getInt("fechadas")));
                    }
                }
            }
            return lista;
        });
    }

    /** OS não fechadas agrupadas por técnico. */
    public Map<String, Integer> abertasPorTecnico(int limite) {
        return Transacao.leitura(c -> {
            Map<String, Integer> mapa = new LinkedHashMap<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COALESCE(tecnico_responsavel, 'Sem técnico') AS tec, COUNT(*) AS n FROM ordens_servico "
                            + "WHERE status <> 'FECHADA' GROUP BY tec ORDER BY n DESC LIMIT ?")) {
                ps.setInt(1, limite);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        mapa.put(rs.getString("tec"), rs.getInt("n"));
                    }
                }
            }
            return mapa;
        });
    }

    /** Valores já usados em uma coluna (para sugestões em listas). */
    public List<String> valoresDistintos(String coluna) {
        if (!COLUNAS_SUGESTAO.contains(coluna)) {
            throw new IllegalArgumentException("Coluna não permitida: " + coluna);
        }
        return Transacao.leitura(c -> {
            List<String> valores = new ArrayList<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT DISTINCT " + coluna + " FROM ordens_servico WHERE " + coluna
                         + " IS NOT NULL AND " + coluna + " <> '' ORDER BY " + coluna + " LIMIT 500")) {
                while (rs.next()) {
                    valores.add(rs.getString(1));
                }
            }
            return valores;
        });
    }

    public List<String> clientesComOSFechadas() {
        return Transacao.leitura(c -> {
            List<String> valores = new ArrayList<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT DISTINCT cliente FROM ordens_servico WHERE status = 'FECHADA' ORDER BY cliente")) {
                while (rs.next()) {
                    valores.add(rs.getString(1));
                }
            }
            return valores;
        });
    }

    // ---------------------------------------------------------------- apoio

    private static boolean preenchido(String s) {
        return s != null && !s.isBlank();
    }

    private static void adicionarLike(StringBuilder where, List<Object> params, String coluna, String valor) {
        if (preenchido(valor)) {
            where.append(" AND ").append(coluna).append(" LIKE ?");
            params.add("%" + valor.trim() + "%");
        }
    }

    private List<OrdemServico> consultar(Connection c, String sql, List<Object> params) throws SQLException {
        List<OrdemServico> lista = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    private OrdemServico mapear(ResultSet rs) throws SQLException {
        OrdemServico os = new OrdemServico();
        os.setId(rs.getLong("id"));
        os.setNumero(rs.getString("numero_os"));
        os.setCliente(rs.getString("cliente"));
        os.setTelefone(rs.getString("telefone"));
        os.setEquipamento(rs.getString("equipamento"));
        os.setModelo(rs.getString("modelo"));
        os.setFabricante(rs.getString("fabricante"));
        os.setPatrimonio(rs.getString("numero_patrimonio"));
        os.setNumeroSerie(rs.getString("numero_serie"));
        os.setStatusEquipamento(rs.getString("status_equipamento"));
        os.setTipoManutencao(rs.getString("tipo_manutencao"));
        os.setTecnico(rs.getString("tecnico_responsavel"));
        os.setValorPrevisto(rs.getBigDecimal("valor_previsto"));
        os.setValorFinal(rs.getBigDecimal("valor_final"));
        os.setDataEntrada(Sql.timestamp(rs, "data_entrada"));
        os.setPrevisaoEntrega(Sql.timestamp(rs, "previsao_entrega"));
        os.setDataEntrega(Sql.timestamp(rs, "data_entrega"));
        os.setProximaPreventiva(Sql.data(rs, "proxima_preventiva"));
        os.setStatus(StatusOS.deTexto(rs.getString("status")));
        os.setDescricaoProblema(rs.getString("descricao_problema"));
        os.setLaudoTecnico(rs.getString("laudo_tecnico"));
        os.setObservacoes(rs.getString("observacoes"));
        os.setRodapeRelatorio(rs.getString("rodape_relatorio"));
        os.setAssinaturaCliente(rs.getString("assinatura_cliente"));
        os.setCriadoEm(Sql.timestamp(rs, "criado_em"));
        os.setAtualizadoEm(Sql.timestamp(rs, "atualizado_em"));
        return os;
    }
}
