package br.opussistem.service;

import br.opussistem.dao.AssinaturaDAO;
import br.opussistem.dao.HistoricoDAO;
import br.opussistem.dao.OrdemServicoDAO;
import br.opussistem.dao.SequenciaDAO;
import br.opussistem.dao.Transacao;
import br.opussistem.exception.AppException;
import br.opussistem.model.CategoriaEvento;
import br.opussistem.model.EventoHistorico;
import br.opussistem.model.FiltroOS;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.StatusOS;
import br.opussistem.model.Usuario;
import br.opussistem.util.Armazenamento;
import br.opussistem.util.Html;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Regras de negócio das ordens de serviço. */
public class OrdemServicoService {

    private final OrdemServicoDAO osDao = new OrdemServicoDAO();
    private final HistoricoDAO historicoDao = new HistoricoDAO();
    private final SequenciaDAO sequenciaDao = new SequenciaDAO();
    private final AssinaturaDAO assinaturaDao = new AssinaturaDAO();

    // ------------------------------------------------------------ criação

    public OrdemServico criar(OrdemServico os, Usuario autor) {
        validar(os);
        return Transacao.executar(c -> {
            verificarSerieDuplicada(c, os, 0);
            os.setNumero(gerarNumero(c));
            os.setStatus(StatusOS.ABERTA);
            os.setDataEntrega(null);
            os.setAssinaturaCliente(null);
            if (os.getDataEntrada() == null) {
                os.setDataEntrada(LocalDateTime.now().withSecond(0).withNano(0));
            }
            long id = osDao.inserir(c, os);
            historicoDao.inserir(c, new EventoHistorico(id, CategoriaEvento.ABERTURA,
                    "OS aberta — Cadastro realizado", "Ordem de serviço cadastrada no sistema.", nomeAutor(autor)));
            return osDao.buscarPorId(c, id).orElseThrow();
        });
    }

    /** Usado pela importação: mantém o número informado quando possível. */
    public OrdemServico criarImportada(OrdemServico os, Usuario autor, String origem) {
        validar(os);
        return Transacao.executar(c -> {
            if (os.getNumero() == null || os.getNumero().isBlank() || os.getNumero().length() > 20
                    || osDao.buscarPorNumero(c, os.getNumero()).isPresent()) {
                os.setNumero(gerarNumero(c));
            }
            if (os.getStatus() == null) {
                os.setStatus(StatusOS.ABERTA);
            }
            if (os.getStatus() == StatusOS.FECHADA && os.getDataEntrega() == null) {
                os.setDataEntrega(os.getDataEntrada());
            }
            long id = osDao.inserir(c, os);
            historicoDao.inserir(c, new EventoHistorico(id, CategoriaEvento.IMPORTACAO,
                    "OS importada", "Importada de " + origem + ".", nomeAutor(autor)));
            return osDao.buscarPorId(c, id).orElseThrow();
        });
    }

    /** Atualiza uma OS existente a partir de dados importados (mantém assinatura e histórico). */
    public OrdemServico atualizarImportada(long id, OrdemServico dados, Usuario autor, String origem) {
        validar(dados);
        return Transacao.executar(c -> {
            OrdemServico atual = osDao.buscarPorId(c, id).orElseThrow(() -> new AppException("OS não encontrada."));
            dados.setId(id);
            dados.setNumero(atual.getNumero());
            dados.setAssinaturaCliente(atual.getAssinaturaCliente());
            if (dados.getRodapeRelatorio() == null || dados.getRodapeRelatorio().isBlank()) {
                dados.setRodapeRelatorio(atual.getRodapeRelatorio());
            }
            if (dados.getStatus() == StatusOS.FECHADA && dados.getDataEntrega() == null) {
                dados.setDataEntrega(atual.getDataEntrega() != null ? atual.getDataEntrega() : dados.getDataEntrada());
            }
            osDao.atualizar(c, dados);
            historicoDao.inserir(c, new EventoHistorico(id, CategoriaEvento.IMPORTACAO,
                    "OS atualizada por importação", "Dados atualizados a partir de " + origem + ".", nomeAutor(autor)));
            return osDao.buscarPorId(c, id).orElseThrow();
        });
    }

    // ------------------------------------------------------------ alteração

    /**
     * Salva alterações de uma OS não fechada. O histórico registra os campos alterados,
     * a nota informada e a mudança de status (fechar grava a data de entrega).
     */
    public OrdemServico atualizar(OrdemServico novo, String nota, Usuario autor) {
        validar(novo);
        if (novo.getId() == null) {
            throw new AppException("OS sem identificador.");
        }
        return Transacao.executar(c -> {
            OrdemServico atual = osDao.buscarPorId(c, novo.getId())
                    .orElseThrow(() -> new AppException("OS não encontrada."));
            if (atual.getStatus() == StatusOS.FECHADA) {
                throw new AppException("Uma OS fechada não pode ser alterada. Reabra a OS primeiro.");
            }
            if (!Objects.equals(vazioParaNulo(atual.getNumeroSerie()), vazioParaNulo(novo.getNumeroSerie()))) {
                verificarSerieDuplicada(c, novo, atual.getId());
            }
            StatusOS statusNovo = novo.getStatus() == null ? atual.getStatus() : novo.getStatus();
            List<String> campos = diferencas(atual, novo);

            novo.setNumero(atual.getNumero());
            novo.setStatus(statusNovo);
            novo.setAssinaturaCliente(atual.getAssinaturaCliente());
            novo.setRodapeRelatorio(atual.getRodapeRelatorio());
            novo.setDataEntrega(statusNovo == StatusOS.FECHADA
                    ? LocalDateTime.now().withNano(0) : atual.getDataEntrega());
            osDao.atualizar(c, novo);

            String descricao = (campos.isEmpty() ? "" : "Campos alterados: " + String.join(", ", campos) + ". ")
                    + (nota == null || nota.isBlank() ? "" : nota.trim());
            if (statusNovo != atual.getStatus()) {
                if (descricao.isBlank()) {
                    descricao = "Status alterado de " + atual.getStatus().getRotulo() + " para " + statusNovo.getRotulo() + ".";
                }
                historicoDao.inserir(c, eventoDeStatus(atual.getId(), statusNovo, descricao, autor));
            } else if (!descricao.isBlank()) {
                historicoDao.inserir(c, new EventoHistorico(atual.getId(), CategoriaEvento.ALTERACAO,
                        "OS alterada", descricao, nomeAutor(autor)));
            }
            return osDao.buscarPorId(c, atual.getId()).orElseThrow();
        });
    }

    /** Fecha a OS (grava a data de entrega e registra no histórico). Não faz nada se já estiver fechada. */
    public OrdemServico fechar(long id, Usuario autor) {
        return Transacao.executar(c -> {
            OrdemServico os = osDao.buscarPorId(c, id).orElseThrow(() -> new AppException("OS não encontrada."));
            if (os.getStatus() == StatusOS.FECHADA) {
                return os;
            }
            osDao.atualizarStatus(c, id, StatusOS.FECHADA, LocalDateTime.now().withNano(0));
            historicoDao.inserir(c, eventoDeStatus(id, StatusOS.FECHADA,
                    "Status alterado de " + os.getStatus().getRotulo() + " para " + StatusOS.FECHADA.getRotulo() + ".", autor));
            return osDao.buscarPorId(c, id).orElseThrow();
        });
    }

    public OrdemServico reabrir(long id, String motivo, Usuario autor) {
        if (motivo == null || motivo.isBlank()) {
            throw new AppException("Informe o motivo da reabertura.");
        }
        return Transacao.executar(c -> {
            OrdemServico os = osDao.buscarPorId(c, id).orElseThrow(() -> new AppException("OS não encontrada."));
            if (os.getStatus() != StatusOS.FECHADA) {
                throw new AppException("Só é possível reabrir uma OS fechada.");
            }
            boolean tinhaAssinatura = !assinaturaDao.listarPorOS(c, id).isEmpty();
            osDao.atualizarStatus(c, id, StatusOS.REABERTA, null);
            if (tinhaAssinatura) {
                assinaturaDao.remover(c, id, br.opussistem.model.PapelAssinatura.CLIENTE);
                assinaturaDao.remover(c, id, br.opussistem.model.PapelAssinatura.TECNICO);
                osDao.atualizarAssinaturaCliente(c, id, null);
            }
            historicoDao.inserir(c, new EventoHistorico(id, CategoriaEvento.REABERTURA, "OS reaberta",
                    motivo.trim() + (tinhaAssinatura ? " As assinaturas anteriores foram removidas." : ""), nomeAutor(autor)));
            return osDao.buscarPorId(c, id).orElseThrow();
        });
    }

    /**
     * Define (ou remove, se vazio) o texto de rodapé do relatório desta OS. Pode ser feito mesmo com a OS
     * fechada, pois o rodapé não faz parte do conteúdo assinado.
     */
    public OrdemServico salvarRodape(long id, String texto, Usuario autor) {
        String novo = texto == null || texto.isBlank() ? null : RodapeService.validar(texto);
        return Transacao.executar(c -> {
            OrdemServico os = osDao.buscarPorId(c, id).orElseThrow(() -> new AppException("OS não encontrada."));
            osDao.atualizarRodape(c, id, novo);
            historicoDao.inserir(c, new EventoHistorico(id, CategoriaEvento.ALTERACAO, "Rodapé do relatório alterado",
                    novo == null ? "O rodapé específico desta OS foi removido." : "Rodapé específico definido para esta OS.",
                    nomeAutor(autor)));
            return osDao.buscarPorId(c, os.getId()).orElseThrow();
        });
    }

    // ------------------------------------------------------------ consultas

    public Optional<OrdemServico> buscar(long id) {
        return osDao.buscarPorId(id);
    }

    public List<OrdemServico> pesquisar(FiltroOS filtro, int limite) {
        return osDao.pesquisar(filtro, limite);
    }

    public List<String> clientesComOSFechadas() {
        return osDao.clientesComOSFechadas();
    }

    public List<EventoHistorico> historico(long osId) {
        return historicoDao.listarPorOS(osId);
    }

    /** Valores já usados em um campo + os cadastrados nas listas do perfil (sem repetição, em ordem alfabética). */
    public List<String> sugestoes(String coluna, List<String> daConfiguracao) {
        java.util.TreeSet<String> conjunto = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        conjunto.addAll(daConfiguracao);
        conjunto.addAll(osDao.valoresDistintos(coluna));
        return new ArrayList<>(conjunto);
    }

    // ------------------------------------------------------------ anexos

    public List<Path> listarAnexos(OrdemServico os) {
        Path pasta = Armazenamento.raiz().resolve("ordens").resolve(Armazenamento.nomeSeguro(os.getNumero()));
        if (!Files.isDirectory(pasta)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(pasta)) {
            return s.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            throw new AppException("Não foi possível listar os anexos: " + e.getMessage(), e);
        }
    }

    public void adicionarAnexos(OrdemServico os, List<Path> arquivos) {
        for (Path origem : arquivos) {
            try {
                if (Files.size(origem) > 10L * 1024 * 1024) {
                    throw new AppException("O arquivo " + origem.getFileName() + " passa de 10 MB.");
                }
            } catch (IOException e) {
                throw new AppException("Não foi possível ler " + origem + ".", e);
            }
            Armazenamento.copiar(origem, Armazenamento.anexos(os.getNumero())
                    .resolve(Armazenamento.nomeSeguro(origem.getFileName().toString())));
        }
    }

    // ------------------------------------------------------------ apoio

    private String gerarNumero(java.sql.Connection c) throws java.sql.SQLException {
        int ano = LocalDateTime.now().getYear();
        return String.format("OS-%d-%04d", ano, sequenciaDao.proximo(c, "OS", ano));
    }

    private void verificarSerieDuplicada(java.sql.Connection c, OrdemServico os, long excluirId) throws java.sql.SQLException {
        String serie = vazioParaNulo(os.getNumeroSerie());
        if (serie != null) {
            Optional<String> existente = osDao.existeAbertaComSerie(c, serie, excluirId);
            if (existente.isPresent()) {
                throw new AppException("Já existe uma OS em aberto (" + existente.get() + ") para o nº de série " + serie + ".");
            }
        }
    }

    static void validar(OrdemServico os) {
        if (vazioParaNulo(os.getCliente()) == null) {
            throw new AppException("Informe o cliente.");
        }
        if (vazioParaNulo(os.getEquipamento()) == null) {
            throw new AppException("Informe o equipamento.");
        }
        if (os.getDataEntrada() == null) {
            throw new AppException("Informe a data de entrada.");
        }
        limite("cliente", os.getCliente(), 150);
        limite("telefone", os.getTelefone(), 30);
        limite("equipamento", os.getEquipamento(), 150);
        limite("modelo", os.getModelo(), 100);
        limite("fabricante", os.getFabricante(), 100);
        limite("patrimônio", os.getPatrimonio(), 50);
        limite("nº de série", os.getNumeroSerie(), 80);
        limite("status do equipamento", os.getStatusEquipamento(), 50);
        limite("tipo de manutenção", os.getTipoManutencao(), 50);
        limite("técnico", os.getTecnico(), 150);
        limite("rodapé", os.getRodapeRelatorio(), RodapeService.LIMITE_CARACTERES);
        if (negativo(os.getValorPrevisto()) || negativo(os.getValorFinal())) {
            throw new AppException("Os valores não podem ser negativos.");
        }
    }

    private static boolean negativo(BigDecimal v) {
        return v != null && v.signum() < 0;
    }

    private static void limite(String campo, String valor, int max) {
        if (valor != null && valor.length() > max) {
            throw new AppException("O campo " + campo + " aceita no máximo " + max + " caracteres.");
        }
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String nomeAutor(Usuario u) {
        return u == null ? "Sistema" : u.getNome();
    }

    private EventoHistorico eventoDeStatus(long osId, StatusOS status, String descricao, Usuario autor) {
        return switch (status) {
            case EM_ANDAMENTO -> new EventoHistorico(osId, CategoriaEvento.ANDAMENTO, "Manutenção iniciada", descricao, nomeAutor(autor));
            case AGUARDANDO_PECA -> new EventoHistorico(osId, CategoriaEvento.AGUARDANDO, "Aguardando peça", descricao, nomeAutor(autor));
            case FECHADA -> new EventoHistorico(osId, CategoriaEvento.FECHAMENTO, "OS fechada — relatório técnico gerado", descricao, nomeAutor(autor));
            case REABERTA -> new EventoHistorico(osId, CategoriaEvento.REABERTURA, "OS reaberta", descricao, nomeAutor(autor));
            case ABERTA -> new EventoHistorico(osId, CategoriaEvento.ALTERACAO, "OS retornou para Aberta", descricao, nomeAutor(autor));
        };
    }

    /** Rótulos dos campos que mudaram entre duas versões da OS. */
    static List<String> diferencas(OrdemServico a, OrdemServico b) {
        List<String> campos = new ArrayList<>();
        comparar(campos, "cliente", a.getCliente(), b.getCliente());
        comparar(campos, "telefone", a.getTelefone(), b.getTelefone());
        comparar(campos, "equipamento", a.getEquipamento(), b.getEquipamento());
        comparar(campos, "modelo", a.getModelo(), b.getModelo());
        comparar(campos, "fabricante", a.getFabricante(), b.getFabricante());
        comparar(campos, "patrimonio", a.getPatrimonio(), b.getPatrimonio());
        comparar(campos, "serie", a.getNumeroSerie(), b.getNumeroSerie());
        comparar(campos, "statusEquipamento", a.getStatusEquipamento(), b.getStatusEquipamento());
        comparar(campos, "manutencao", a.getTipoManutencao(), b.getTipoManutencao());
        comparar(campos, "tecnico", a.getTecnico(), b.getTecnico());
        if (!Objects.equals(a.getDataEntrada(), b.getDataEntrada())) {
            campos.add("Data de entrada");
        }
        if (!Objects.equals(a.getPrevisaoEntrega(), b.getPrevisaoEntrega())) {
            campos.add("Previsão de entrega");
        }
        if (!Objects.equals(a.getProximaPreventiva(), b.getProximaPreventiva())) {
            campos.add("Próxima preventiva");
        }
        if (compararDecimal(a.getValorPrevisto(), b.getValorPrevisto())) {
            campos.add("Valor previsto");
        }
        if (compararDecimal(a.getValorFinal(), b.getValorFinal())) {
            campos.add("Valor final");
        }
        if (!Html.paraTexto(a.getDescricaoProblema()).equals(Html.paraTexto(b.getDescricaoProblema()))) {
            campos.add(Rotulos.de("natureza"));
        }
        if (!Html.paraTexto(a.getLaudoTecnico()).equals(Html.paraTexto(b.getLaudoTecnico()))) {
            campos.add(Rotulos.de("laudo"));
        }
        if (!Html.paraTexto(a.getObservacoes()).equals(Html.paraTexto(b.getObservacoes()))) {
            campos.add(Rotulos.de("observacoes"));
        }
        return campos;
    }

    private static void comparar(List<String> campos, String chaveRotulo, String antes, String depois) {
        if (!Objects.equals(vazioParaNulo(antes), vazioParaNulo(depois))) {
            campos.add(Rotulos.de(chaveRotulo));
        }
    }

    private static boolean compararDecimal(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a != b;
        }
        return a.compareTo(b) != 0;
    }
}
