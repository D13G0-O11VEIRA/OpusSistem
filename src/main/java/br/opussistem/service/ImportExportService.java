package br.opussistem.service;

import br.opussistem.dao.OrdemServicoDAO;
import br.opussistem.dao.Transacao;
import br.opussistem.exception.AppException;
import br.opussistem.model.EventoHistorico;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.StatusOS;
import br.opussistem.model.Usuario;
import br.opussistem.util.Csv;
import br.opussistem.util.Formatos;
import br.opussistem.util.Html;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Exporta OS para CSV/JSON e importa de CSV/JSON de outros sistemas. A importação sugere
 * automaticamente a correspondência entre as colunas do arquivo e os campos do OpusSistem
 * (o usuário pode ajustar), por isso funciona com layouts diferentes.
 */
public class ImportExportService {

    /** Campos que podem ser importados, com sinônimos comuns em outros sistemas (já normalizados). */
    public enum Campo {
        NUMERO("Número da OS", "numero", "numero os", "n os", "os", "numero", "codigo", "id", "ordem de servico", "ordem"),
        CLIENTE("Cliente", "cliente", "nome cliente", "proprietario", "contratante"),
        TELEFONE("Telefone", "telefone", "fone", "celular", "contato"),
        EQUIPAMENTO("Equipamento", "equipamento", "tipo equipamento", "veiculo", "tipo veiculo", "categoria", "tipo"),
        MODELO("Modelo", "modelo", "versao"),
        FABRICANTE("Fabricante", "fabricante", "marca", "montadora"),
        PATRIMONIO("Patrimônio", "patrimonio", "placa", "tag"),
        SERIE("Nº de série", "numeroSerie", "serie", "numero de serie", "n serie", "chassi", "imei"),
        STATUS_EQUIPAMENTO("Status do equipamento", "statusEquipamento", "status equipamento", "condicao"),
        ENTRADA("Data de entrada", "dataEntrada", "data entrada", "entrada", "recebimento", "data recebimento", "abertura", "data abertura", "data"),
        MANUTENCAO("Tipo de manutenção", "tipoManutencao", "tipo manutencao", "manutencao", "tipo servico"),
        PREVISAO("Previsão de entrega", "previsaoEntrega", "previsao entrega", "previsao", "prazo"),
        PROXIMA_PREVENTIVA("Próxima preventiva", "proximaPreventiva", "proxima preventiva", "prox preventiva", "retorno", "proxima revisao"),
        TECNICO("Técnico", "tecnico", "tecnico responsavel", "mecanico", "responsavel", "funcionario", "atendente", "executor"),
        NATUREZA("Natureza do serviço", "natureza", "natureza servico", "servico", "servicos", "servicos solicitados", "defeito", "reclamacao", "solicitacao", "problema", "descricao problema"),
        LAUDO("Laudo técnico", "laudo", "laudo tecnico", "diagnostico", "parecer"),
        OBSERVACOES("Observações", "observacoes", "observacao", "obs"),
        VALOR_PREVISTO("Valor previsto", "valorPrevisto", "valor previsto", "orcamento"),
        VALOR("Valor final", "valorFinal", "valor final", "valor", "valor total", "total", "preco"),
        SAIDA("Data de saída", "dataEntrega", "data entrega", "saida", "data saida", "fechamento", "data fechamento", "conclusao", "entrega", "encerramento"),
        STATUS("Status da OS", "status", "situacao", "estado", "status os"),
        RODAPE("Rodapé do relatório", "rodapeRelatorio", "rodape relatorio", "rodape", "rodape da folha", "texto do rodape");

        private final String rotulo;
        private final String chaveJson;
        private final List<String> sinonimos;

        Campo(String rotulo, String chaveJson, String... sinonimos) {
            this.rotulo = rotulo;
            this.chaveJson = chaveJson;
            this.sinonimos = List.of(sinonimos);
        }

        public String rotulo() {
            return rotulo;
        }
    }

    /** Ordem de busca: campos mais específicos primeiro (evita "status equipamento" virar "status"). */
    private static final List<Campo> ORDEM_BUSCA = List.of(
            Campo.STATUS_EQUIPAMENTO, Campo.MANUTENCAO, Campo.PROXIMA_PREVENTIVA, Campo.PREVISAO, Campo.SAIDA,
            Campo.VALOR_PREVISTO, Campo.ENTRADA, Campo.SERIE, Campo.PATRIMONIO, Campo.NUMERO, Campo.STATUS,
            Campo.CLIENTE, Campo.TELEFONE, Campo.EQUIPAMENTO, Campo.MODELO, Campo.FABRICANTE, Campo.TECNICO,
            Campo.RODAPE, Campo.NATUREZA, Campo.LAUDO, Campo.OBSERVACOES, Campo.VALOR);

    public enum ModoConflito { IGNORAR, ATUALIZAR, CRIAR_NOVA }

    public record ArquivoImportacao(String nome, List<String> colunas, List<Map<String, String>> linhas, boolean formatoNativo) {
    }

    public record ResultadoImportacao(int criadas, int atualizadas, int ignoradas, List<String> erros) {
    }

    private final OrdemServicoDAO osDao = new OrdemServicoDAO();
    private final OrdemServicoService osService;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public ImportExportService(OrdemServicoService osService) {
        this.osService = osService;
    }

    // ------------------------------------------------------------------ exportação

    public String paraCsv(List<OrdemServico> lista) {
        List<String[]> linhas = new ArrayList<>();
        Campo[] campos = Campo.values();
        String[] cabecalho = new String[campos.length];
        for (int i = 0; i < campos.length; i++) {
            cabecalho[i] = campos[i].rotulo();
        }
        linhas.add(cabecalho);
        for (OrdemServico os : lista) {
            String[] linha = new String[campos.length];
            for (int i = 0; i < campos.length; i++) {
                linha[i] = valorCsv(os, campos[i]);
            }
            linhas.add(linha);
        }
        return Csv.escrever(linhas);
    }

    private static String valorCsv(OrdemServico os, Campo campo) {
        return switch (campo) {
            case NUMERO -> os.getNumero();
            case CLIENTE -> os.getCliente();
            case TELEFONE -> os.getTelefone();
            case EQUIPAMENTO -> os.getEquipamento();
            case MODELO -> os.getModelo();
            case FABRICANTE -> os.getFabricante();
            case PATRIMONIO -> os.getPatrimonio();
            case SERIE -> os.getNumeroSerie();
            case STATUS_EQUIPAMENTO -> os.getStatusEquipamento();
            case ENTRADA -> Formatos.data(os.getDataEntrada());
            case MANUTENCAO -> os.getTipoManutencao();
            case PREVISAO -> Formatos.data(os.getPrevisaoEntrega());
            case PROXIMA_PREVENTIVA -> Formatos.data(os.getProximaPreventiva());
            case TECNICO -> os.getTecnico();
            case NATUREZA -> Html.paraTexto(os.getDescricaoProblema());
            case LAUDO -> Html.paraTexto(os.getLaudoTecnico());
            case OBSERVACOES -> Html.paraTexto(os.getObservacoes());
            case VALOR_PREVISTO -> Formatos.decimal(os.getValorPrevisto());
            case VALOR -> Formatos.decimal(os.getValorFinal());
            case SAIDA -> Formatos.data(os.getDataEntrega());
            case STATUS -> os.getStatus().getRotulo();
            case RODAPE -> os.getRodapeRelatorio();
        };
    }

    /** JSON no formato do OpusSistem (reimportável): OS com histórico. */
    public String paraJson(List<OrdemServico> lista, Map<String, String> configuracoes) {
        JsonObject raiz = new JsonObject();
        raiz.addProperty("formato", "ordens-de-servico");
        raiz.addProperty("versao", 1);
        raiz.addProperty("exportadoEm", LocalDateTime.now().withNano(0).toString());
        JsonArray array = new JsonArray();
        for (OrdemServico os : lista) {
            JsonObject o = new JsonObject();
            o.addProperty("numero", os.getNumero());
            o.addProperty("cliente", os.getCliente());
            o.addProperty("telefone", os.getTelefone());
            o.addProperty("equipamento", os.getEquipamento());
            o.addProperty("modelo", os.getModelo());
            o.addProperty("fabricante", os.getFabricante());
            o.addProperty("patrimonio", os.getPatrimonio());
            o.addProperty("numeroSerie", os.getNumeroSerie());
            o.addProperty("statusEquipamento", os.getStatusEquipamento());
            o.addProperty("tipoManutencao", os.getTipoManutencao());
            o.addProperty("tecnico", os.getTecnico());
            o.addProperty("valorPrevisto", os.getValorPrevisto());
            o.addProperty("valorFinal", os.getValorFinal());
            o.addProperty("dataEntrada", texto(os.getDataEntrada()));
            o.addProperty("previsaoEntrega", texto(os.getPrevisaoEntrega()));
            o.addProperty("dataEntrega", texto(os.getDataEntrega()));
            o.addProperty("proximaPreventiva", texto(os.getProximaPreventiva()));
            o.addProperty("status", os.getStatus().name());
            o.addProperty("natureza", os.getDescricaoProblema());
            o.addProperty("laudo", os.getLaudoTecnico());
            o.addProperty("observacoes", os.getObservacoes());
            o.addProperty("rodapeRelatorio", os.getRodapeRelatorio());
            JsonArray historico = new JsonArray();
            for (EventoHistorico e : osService.historico(os.getId())) {
                JsonObject h = new JsonObject();
                h.addProperty("dataHora", texto(e.getDataHora()));
                h.addProperty("categoria", e.getCategoria().name());
                h.addProperty("titulo", e.getTitulo());
                h.addProperty("descricao", e.getDescricao());
                h.addProperty("responsavel", e.getResponsavel());
                historico.add(h);
            }
            o.add("historico", historico);
            array.add(o);
        }
        raiz.add("os", array);
        if (configuracoes != null) {
            JsonObject cfg = new JsonObject();
            configuracoes.forEach(cfg::addProperty);
            raiz.add("configuracoes", cfg);
        }
        return gson.toJson(raiz);
    }

    public void gravar(Path destino, String conteudo) {
        try {
            Files.writeString(destino, conteudo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AppException("Não foi possível gravar o arquivo: " + e.getMessage(), e);
        }
    }

    /** Arquivo CSV só com o cabeçalho (modelo para preencher e importar). */
    public String modeloCsv() {
        String[] cab = new String[Campo.values().length];
        for (int i = 0; i < cab.length; i++) {
            cab[i] = Campo.values()[i].rotulo();
        }
        return Csv.escrever(List.<String[]>of(cab));
    }

    // ------------------------------------------------------------------ importação

    public ArquivoImportacao ler(Path arquivo) {
        String texto = decodificar(arquivo);
        String inicio = texto.stripLeading();
        String nome = arquivo.getFileName().toString();
        if (inicio.startsWith("{") || inicio.startsWith("[")) {
            return lerJson(nome, inicio);
        }
        if (inicio.startsWith("<")) {
            throw new AppException("Arquivos XML ainda não são suportados. Exporte do outro sistema como CSV ou JSON.");
        }
        List<String[]> linhas = Csv.ler(texto);
        if (linhas.size() < 2) {
            throw new AppException("O arquivo não tem linhas de dados.");
        }
        String[] cab = linhas.get(0);
        List<String> colunas = new ArrayList<>();
        for (int i = 0; i < cab.length; i++) {
            colunas.add(cab[i].isBlank() ? "coluna " + (i + 1) : cab[i].trim());
        }
        List<Map<String, String>> dados = new ArrayList<>();
        for (String[] l : linhas.subList(1, linhas.size())) {
            Map<String, String> linha = new LinkedHashMap<>();
            for (int i = 0; i < colunas.size(); i++) {
                linha.put(colunas.get(i), i < l.length ? l[i].trim() : "");
            }
            dados.add(linha);
        }
        return new ArquivoImportacao(nome, colunas, dados, false);
    }

    private ArquivoImportacao lerJson(String nome, String texto) {
        JsonElement raiz;
        try {
            raiz = JsonParser.parseString(texto);
        } catch (RuntimeException e) {
            throw new AppException("JSON inválido: " + e.getMessage(), e);
        }
        boolean nativo = raiz.isJsonObject() && raiz.getAsJsonObject().has("formato")
                && "ordens-de-servico".equals(raiz.getAsJsonObject().get("formato").getAsString());
        JsonArray array = null;
        if (raiz.isJsonArray()) {
            array = raiz.getAsJsonArray();
        } else if (raiz.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : raiz.getAsJsonObject().entrySet()) {
                if (e.getValue().isJsonArray()) {
                    array = e.getValue().getAsJsonArray();
                    break;
                }
            }
        }
        if (array == null || array.isEmpty()) {
            throw new AppException("Nenhuma lista de OS encontrada no JSON.");
        }
        List<Map<String, String>> linhas = new ArrayList<>();
        Set<String> colunas = new java.util.LinkedHashSet<>();
        for (JsonElement el : array) {
            if (el.isJsonObject()) {
                Map<String, String> linha = new LinkedHashMap<>();
                achatar(el.getAsJsonObject(), linha);
                colunas.addAll(linha.keySet());
                linhas.add(linha);
            }
        }
        return new ArquivoImportacao(nome, new ArrayList<>(colunas), linhas, nativo);
    }

    private void achatar(JsonObject objeto, Map<String, String> saida) {
        for (Map.Entry<String, JsonElement> e : objeto.entrySet()) {
            JsonElement v = e.getValue();
            if (v.isJsonPrimitive()) {
                saida.put(e.getKey(), v.getAsString());
            } else if (v.isJsonObject()) {
                achatar(v.getAsJsonObject(), saida);
            }
            // listas (ex.: historico) são ignoradas
        }
    }

    /** Sugere qual coluna do arquivo corresponde a cada campo (uma coluna só serve a um campo). */
    public Map<Campo, String> sugerirMapeamento(List<String> colunas) {
        Map<Campo, String> mapa = new EnumMap<>(Campo.class);
        Set<Integer> usadas = new HashSet<>();
        List<String> normalizadas = colunas.stream().map(ImportExportService::normalizar).toList();
        for (Campo campo : ORDEM_BUSCA) {
            List<String> nomes = new ArrayList<>();
            nomes.add(normalizar(campo.rotulo));
            nomes.add(normalizar(campo.chaveJson));
            campo.sinonimos.forEach(s -> nomes.add(normalizar(s)));
            int achado = -1;
            for (int i = 0; i < normalizadas.size() && achado < 0; i++) {
                if (!usadas.contains(i) && !normalizadas.get(i).isEmpty() && nomes.contains(normalizadas.get(i))) {
                    achado = i;
                }
            }
            for (int i = 0; i < normalizadas.size() && achado < 0; i++) {
                String col = normalizadas.get(i);
                if (!usadas.contains(i) && !col.isEmpty() && nomes.stream().anyMatch(n -> n.length() > 3 && col.contains(n))) {
                    achado = i;
                }
            }
            if (achado >= 0) {
                mapa.put(campo, colunas.get(achado));
                usadas.add(achado);
            }
        }
        return mapa;
    }

    /** Converte as linhas do arquivo em OS usando o mapeamento escolhido. */
    public List<OrdemServico> converter(ArquivoImportacao arquivo, Map<Campo, String> mapa, StatusOS statusPadrao) {
        List<OrdemServico> lista = new ArrayList<>();
        for (Map<String, String> linha : arquivo.linhas()) {
            lista.add(converterLinha(linha, mapa, statusPadrao));
        }
        return lista;
    }

    OrdemServico converterLinha(Map<String, String> linha, Map<Campo, String> mapa, StatusOS statusPadrao) {
        OrdemServico os = new OrdemServico();
        os.setNumero(valor(linha, mapa, Campo.NUMERO));
        os.setCliente(padrao(valor(linha, mapa, Campo.CLIENTE), "(não informado)"));
        os.setTelefone(valor(linha, mapa, Campo.TELEFONE));
        os.setEquipamento(padrao(valor(linha, mapa, Campo.EQUIPAMENTO), "(não informado)"));
        os.setModelo(valor(linha, mapa, Campo.MODELO));
        os.setFabricante(valor(linha, mapa, Campo.FABRICANTE));
        os.setPatrimonio(valor(linha, mapa, Campo.PATRIMONIO));
        os.setNumeroSerie(valor(linha, mapa, Campo.SERIE));
        os.setStatusEquipamento(valor(linha, mapa, Campo.STATUS_EQUIPAMENTO));
        os.setTipoManutencao(valor(linha, mapa, Campo.MANUTENCAO));
        os.setTecnico(valor(linha, mapa, Campo.TECNICO));
        os.setValorPrevisto(Formatos.lerDecimal(valor(linha, mapa, Campo.VALOR_PREVISTO)));
        os.setValorFinal(Formatos.lerDecimal(valor(linha, mapa, Campo.VALOR)));
        var entrada = Formatos.lerData(valor(linha, mapa, Campo.ENTRADA));
        os.setDataEntrada(entrada == null ? LocalDateTime.now().withSecond(0).withNano(0) : entrada.atStartOfDay());
        var previsao = Formatos.lerData(valor(linha, mapa, Campo.PREVISAO));
        os.setPrevisaoEntrega(previsao == null ? null : previsao.atStartOfDay());
        var saida = Formatos.lerData(valor(linha, mapa, Campo.SAIDA));
        os.setDataEntrega(saida == null ? null : saida.atStartOfDay());
        os.setProximaPreventiva(Formatos.lerData(valor(linha, mapa, Campo.PROXIMA_PREVENTIVA)));
        os.setDescricaoProblema(Html.deTexto(valor(linha, mapa, Campo.NATUREZA)));
        os.setLaudoTecnico(Html.deTexto(valor(linha, mapa, Campo.LAUDO)));
        os.setObservacoes(Html.deTexto(valor(linha, mapa, Campo.OBSERVACOES)));
        os.setRodapeRelatorio(valor(linha, mapa, Campo.RODAPE));
        String status = valor(linha, mapa, Campo.STATUS);
        if (status != null) {
            os.setStatus(StatusOS.deTexto(status));
        } else if (saida != null) {
            os.setStatus(StatusOS.FECHADA);
        } else {
            os.setStatus(statusPadrao == null ? StatusOS.ABERTA : statusPadrao);
        }
        return os;
    }

    public ResultadoImportacao importar(List<OrdemServico> lista, ModoConflito modo, Usuario autor, String origem) {
        int criadas = 0;
        int atualizadas = 0;
        int ignoradas = 0;
        List<String> erros = new ArrayList<>();
        int posicao = 0;
        for (OrdemServico os : lista) {
            posicao++;
            try {
                Optional<OrdemServico> existente = os.getNumero() == null ? Optional.empty()
                        : Transacao.leitura(c -> osDao.buscarPorNumero(c, os.getNumero()));
                if (existente.isPresent()) {
                    switch (modo) {
                        case IGNORAR -> ignoradas++;
                        case ATUALIZAR -> {
                            osService.atualizarImportada(existente.get().getId(), os, autor, origem);
                            atualizadas++;
                        }
                        case CRIAR_NOVA -> {
                            os.setNumero(null);
                            osService.criarImportada(os, autor, origem);
                            criadas++;
                        }
                    }
                } else {
                    osService.criarImportada(os, autor, origem);
                    criadas++;
                }
            } catch (RuntimeException e) {
                erros.add("Linha " + posicao + ": " + e.getMessage());
            }
        }
        return new ResultadoImportacao(criadas, atualizadas, ignoradas, erros);
    }

    // ------------------------------------------------------------------ apoio

    private static String valor(Map<String, String> linha, Map<Campo, String> mapa, Campo campo) {
        String coluna = mapa.get(campo);
        if (coluna == null) {
            return null;
        }
        String v = linha.get(coluna);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String padrao(String v, String padrao) {
        return v == null ? padrao : v;
    }

    private static String texto(Object o) {
        return o == null ? null : o.toString();
    }

    /** UTF-8; se o arquivo não for UTF-8 válido, assume Windows-1252 (comum em sistemas brasileiros). */
    private static String decodificar(Path arquivo) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(arquivo);
        } catch (IOException e) {
            throw new AppException("Não foi possível ler o arquivo: " + e.getMessage(), e);
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, java.nio.charset.Charset.forName("windows-1252"));
        }
    }

    static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String separado = s.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Normalizer.normalize(separado, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }
}
