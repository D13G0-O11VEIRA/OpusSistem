package br.opussistem.service;

import br.opussistem.dao.RodapeClienteDAO;
import br.opussistem.exception.AppException;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.RodapeCliente;

import java.util.List;
import java.util.Optional;

/**
 * Texto no rodapé da folha final do relatório (por exemplo, o e-mail que um cliente pede para
 * constar no pé da página). Ordem de prioridade: rodapé da própria OS, rodapé do cliente e,
 * por último, o rodapé geral de Perfil > Empresa e relatório.
 */
public class RodapeService {

    public static final int LIMITE_CARACTERES = 1000;

    /** De onde veio o rodapé usado em um relatório. */
    public enum Origem {
        OS("desta OS"),
        CLIENTE("do cliente"),
        GERAL("geral (Perfil)"),
        NENHUM("nenhum");

        private final String rotulo;

        Origem(String rotulo) {
            this.rotulo = rotulo;
        }

        public String getRotulo() {
            return rotulo;
        }
    }

    public record Efetivo(String texto, Origem origem) {

        public boolean vazio() {
            return texto == null || texto.isBlank();
        }
    }

    private final RodapeClienteDAO dao = new RodapeClienteDAO();
    private final ConfiguracaoService config = ConfiguracaoService.instancia();

    /** Rodapé que vale para o relatório desta OS. */
    public Efetivo efetivo(OrdemServico os) {
        return escolher(os.getRodapeRelatorio(), doCliente(os.getCliente()).orElse(null),
                config.get(ConfiguracaoService.REL_RODAPE));
    }

    /** Rodapé que vale para documentos do cliente em geral (capa de pacote, por exemplo). */
    public Efetivo efetivoDoCliente(String cliente) {
        return escolher(null, doCliente(cliente).orElse(null), config.get(ConfiguracaoService.REL_RODAPE));
    }

    /** Regra de prioridade, sem acesso ao banco. */
    static Efetivo escolher(String daOs, String doCliente, String geral) {
        if (preenchido(daOs)) {
            return new Efetivo(normalizar(daOs), Origem.OS);
        }
        if (preenchido(doCliente)) {
            return new Efetivo(normalizar(doCliente), Origem.CLIENTE);
        }
        if (preenchido(geral)) {
            return new Efetivo(normalizar(geral), Origem.GERAL);
        }
        return new Efetivo("", Origem.NENHUM);
    }

    public Optional<String> doCliente(String cliente) {
        if (cliente == null || cliente.isBlank()) {
            return Optional.empty();
        }
        return dao.buscar(cliente.trim()).filter(RodapeService::preenchido);
    }

    public List<RodapeCliente> listar() {
        return dao.listar();
    }

    /** Salva o rodapé do cliente; texto vazio remove o rodapé específico. */
    public void salvarDoCliente(String cliente, String texto) {
        if (cliente == null || cliente.isBlank()) {
            throw new AppException("Informe o nome do cliente.");
        }
        if (cliente.trim().length() > 150) {
            throw new AppException("O nome do cliente aceita no máximo 150 caracteres.");
        }
        if (!preenchido(texto)) {
            dao.remover(cliente.trim());
            return;
        }
        dao.salvar(cliente.trim(), validar(texto));
    }

    public void removerDoCliente(String cliente) {
        if (cliente != null && !cliente.isBlank()) {
            dao.remover(cliente.trim());
        }
    }

    /** Valida e normaliza o texto (usado também para o rodapé de uma OS). */
    public static String validar(String texto) {
        String t = normalizar(texto);
        if (t.length() > LIMITE_CARACTERES) {
            throw new AppException("O rodapé aceita no máximo " + LIMITE_CARACTERES + " caracteres.");
        }
        return t;
    }

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String linha : texto.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            sb.append(linha.stripTrailing()).append('\n');
        }
        return sb.toString().strip();
    }

    private static boolean preenchido(String s) {
        return s != null && !s.isBlank();
    }
}
