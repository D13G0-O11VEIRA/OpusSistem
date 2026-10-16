package br.opussistem.service;

import br.opussistem.dao.AssinaturaDAO;
import br.opussistem.dao.HistoricoDAO;
import br.opussistem.dao.OrdemServicoDAO;
import br.opussistem.dao.Transacao;
import br.opussistem.exception.AppException;
import br.opussistem.model.Assinatura;
import br.opussistem.model.CategoriaEvento;
import br.opussistem.model.EventoHistorico;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.PacoteAssinatura;
import br.opussistem.model.PapelAssinatura;
import br.opussistem.model.StatusOS;
import br.opussistem.model.Usuario;
import br.opussistem.util.Armazenamento;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Assinatura digital (imagem desenhada) individual e em lote. */
public class AssinaturaService {

    private final AssinaturaDAO assinaturaDao = new AssinaturaDAO();
    private final OrdemServicoDAO osDao = new OrdemServicoDAO();
    private final HistoricoDAO historicoDao = new HistoricoDAO();

    public Assinatura assinar(long osId, PapelAssinatura papel, String nome, String email, byte[] png, Usuario autor) {
        validarDados(nome, png);
        return Transacao.executar(c -> {
            OrdemServico os = osDao.buscarPorId(c, osId).orElseThrow(() -> new AppException("OS não encontrada."));
            exigirFechada(os);
            String arquivo = nomeArquivo(os.getNumero() + "_" + papel.name().toLowerCase());
            Armazenamento.gravar(Armazenamento.assinaturas().resolve(arquivo), png);

            Assinatura a = nova(os, papel, nome, email, arquivo, null);
            assinaturaDao.substituir(c, a);
            if (papel == PapelAssinatura.CLIENTE) {
                osDao.atualizarAssinaturaCliente(c, osId, arquivo);
            }
            historicoDao.inserir(c, new EventoHistorico(osId, CategoriaEvento.ASSINATURA, "Relatório assinado digitalmente",
                    "Assinado por " + nome.trim() + " (" + (papel == PapelAssinatura.CLIENTE ? "cliente" : "técnico") + ").",
                    autor == null ? "Sistema" : autor.getNome()));
            return a;
        });
    }

    /** O cliente assina uma única vez e a mesma assinatura vale para todas as OS do pacote. */
    public PacoteAssinatura assinarPacote(List<Long> osIds, String nome, String email, byte[] png, Usuario autor) {
        validarDados(nome, png);
        if (osIds == null || osIds.isEmpty()) {
            throw new AppException("Selecione ao menos uma OS.");
        }
        return Transacao.executar(c -> {
            String codigo = "PK" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
            String arquivo = nomeArquivo("pacote-" + codigo);
            String cliente = null;
            LocalDateTime agora = LocalDateTime.now().withNano(0);
            Armazenamento.gravar(Armazenamento.assinaturas().resolve(arquivo), png);
            for (long id : osIds) {
                OrdemServico os = osDao.buscarPorId(c, id).orElseThrow(() -> new AppException("OS não encontrada."));
                exigirFechada(os);
                if (cliente == null) {
                    cliente = os.getCliente();
                } else if (!cliente.equalsIgnoreCase(os.getCliente())) {
                    throw new AppException("Todas as OS do pacote devem ser do mesmo cliente.");
                }
                assinaturaDao.substituir(c, nova(os, PapelAssinatura.CLIENTE, nome, email, arquivo, codigo));
                osDao.atualizarAssinaturaCliente(c, id, arquivo);
                historicoDao.inserir(c, new EventoHistorico(id, CategoriaEvento.ASSINATURA, "Relatório assinado digitalmente",
                        "Assinado por " + nome.trim() + " (cliente) no pacote " + codigo + ".",
                        autor == null ? "Sistema" : autor.getNome()));
            }
            PacoteAssinatura pacote = new PacoteAssinatura(codigo, cliente, nome.trim(), email, osIds.size(), agora);
            assinaturaDao.inserirPacote(c, pacote);
            return pacote;
        });
    }

    public void remover(long osId, PapelAssinatura papel, Usuario autor) {
        Transacao.executar(c -> {
            assinaturaDao.remover(c, osId, papel);
            if (papel == PapelAssinatura.CLIENTE) {
                osDao.atualizarAssinaturaCliente(c, osId, null);
            }
            historicoDao.inserir(c, new EventoHistorico(osId, CategoriaEvento.ALTERACAO, "Assinatura removida",
                    "Assinatura do " + (papel == PapelAssinatura.CLIENTE ? "cliente" : "técnico") + " removida.",
                    autor == null ? "Sistema" : autor.getNome()));
            return null;
        });
    }

    public Map<PapelAssinatura, Assinatura> daOS(long osId) {
        Map<PapelAssinatura, Assinatura> mapa = new EnumMap<>(PapelAssinatura.class);
        for (Assinatura a : assinaturaDao.listarPorOS(osId)) {
            mapa.put(a.getPapel(), a);
        }
        return mapa;
    }

    /** A assinatura continua válida se o conteúdo da OS não mudou depois de assinada. */
    public boolean valida(Assinatura a, OrdemServico os) {
        return a != null && Integridade.codigo(os).equals(a.getCodigoVerificacao());
    }

    public Optional<byte[]> imagem(Assinatura a) {
        if (a == null || a.getArquivo() == null) {
            return Optional.empty();
        }
        Path arquivo = Armazenamento.assinaturas().resolve(a.getArquivo());
        return Files.exists(arquivo) ? Optional.of(Armazenamento.ler(arquivo)) : Optional.empty();
    }

    public List<PacoteAssinatura> pacotes(int limite) {
        return assinaturaDao.listarPacotes(limite);
    }

    public List<Long> osDoPacote(String codigo) {
        return assinaturaDao.osDoPacote(codigo);
    }

    // ------------------------------------------------------------ apoio

    private Assinatura nova(OrdemServico os, PapelAssinatura papel, String nome, String email, String arquivo, String pacote) {
        Assinatura a = new Assinatura();
        a.setOsId(os.getId());
        a.setPapel(papel);
        a.setNome(nome.trim());
        a.setEmail(email == null || email.isBlank() ? null : email.trim());
        a.setArquivo(arquivo);
        a.setCodigoVerificacao(Integridade.codigo(os));
        a.setPacoteCodigo(pacote);
        a.setAssinadoEm(LocalDateTime.now().withNano(0));
        return a;
    }

    private static void validarDados(String nome, byte[] png) {
        if (nome == null || nome.isBlank()) {
            throw new AppException("Informe o nome de quem assina.");
        }
        if (png == null || png.length == 0) {
            throw new AppException("Desenhe a assinatura no quadro.");
        }
    }

    private static void exigirFechada(OrdemServico os) {
        if (os.getStatus() != StatusOS.FECHADA) {
            throw new AppException("Feche a OS " + os.getNumero() + " antes de assinar o relatório.");
        }
    }

    private static String nomeArquivo(String base) {
        return Armazenamento.nomeSeguro(base) + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".png";
    }
}
