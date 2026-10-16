package br.opussistem.service;

import br.opussistem.dao.ConfiguracaoDAO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurações do sistema guardadas no banco (tabela configuracoes) com cache em memória:
 * dados da empresa, relatório, padrões da OS, listas, alertas e aparência.
 */
public class ConfiguracaoService {

    // Empresa (cabeçalho do relatório)
    public static final String EMPRESA_RAZAO = "empresa.razao";
    public static final String EMPRESA_FANTASIA = "empresa.fantasia";
    public static final String EMPRESA_CNPJ = "empresa.cnpj";
    public static final String EMPRESA_IE = "empresa.ie";
    public static final String EMPRESA_TELEFONE = "empresa.telefone";
    public static final String EMPRESA_EMAIL = "empresa.email";
    public static final String EMPRESA_ENDERECO = "empresa.endereco";
    public static final String EMPRESA_REGISTROS = "empresa.registros";
    // Relatório
    public static final String REL_TITULO = "relatorio.titulo";
    public static final String REL_ASSINATURA1 = "relatorio.assinatura1";
    public static final String REL_ASSINATURA2 = "relatorio.assinatura2";
    public static final String REL_VALIDADE_DIAS = "relatorio.validade.dias";
    public static final String REL_VALIDADE_ROTULO = "relatorio.validade.rotulo";
    public static final String REL_RODAPE = "relatorio.rodape";
    // Padrões de uma nova OS
    public static final String PAD_EQUIPAMENTO = "padrao.equipamento";
    public static final String PAD_FABRICANTE = "padrao.fabricante";
    public static final String PAD_TECNICO = "padrao.tecnico";
    public static final String PAD_MANUTENCAO = "padrao.manutencao";
    public static final String PAD_STATUS_EQUIPAMENTO = "padrao.statusEquipamento";
    public static final String PAD_DIAS_PREVENTIVA = "padrao.diasPreventiva";
    public static final String PAD_NATUREZA = "padrao.natureza";
    public static final String PAD_LAUDO = "padrao.laudo";
    public static final String PAD_OBSERVACOES = "padrao.observacoes";
    // Listas de sugestões (um item por linha)
    public static final String LISTA_EQUIPAMENTOS = "lista.equipamentos";
    public static final String LISTA_FABRICANTES = "lista.fabricantes";
    public static final String LISTA_TECNICOS = "lista.tecnicos";
    public static final String LISTA_MANUTENCAO = "lista.manutencao";
    public static final String LISTA_STATUS_EQUIPAMENTO = "lista.statusEquipamento";
    // Alertas de preventiva
    public static final String ALERTA_1_MES = "alerta.m1";
    public static final String ALERTA_2_MESES = "alerta.m2";
    public static final String ALERTA_3_MESES = "alerta.m3";
    public static final String ALERTA_VALIDADE_DIAS = "alerta.dias";
    // Aparência
    public static final String APP_TITULO = "app.titulo";
    public static final String TEMA = "aparencia.tema";
    public static final String COR_DESTAQUE = "aparencia.corDestaque";
    public static final String COR_MENU = "aparencia.corMenu";

    private static final Map<String, String> PADROES = new HashMap<>();

    static {
        PADROES.put(REL_TITULO, "Relatório de Atendimento Técnico");
        PADROES.put(REL_ASSINATURA1, "Cliente");
        PADROES.put(REL_ASSINATURA2, "Técnico");
        PADROES.put(REL_VALIDADE_DIAS, "365");
        PADROES.put(REL_VALIDADE_ROTULO, "Calibração expira");
        PADROES.put(PAD_DIAS_PREVENTIVA, "180");
        PADROES.put(LISTA_MANUTENCAO, "Preventiva\nCorretiva\nPreventiva/Corretiva\nPreditiva\nInspeção\nCalibração\nInstalação");
        PADROES.put(LISTA_STATUS_EQUIPAMENTO, "Operante\nInoperante\nObsoleto\nAguardando peça\nEm observação\n"
                + "Em manutenção\nEm calibração\nEm garantia\nFora de uso\nBaixado");
        PADROES.put(ALERTA_1_MES, "true");
        PADROES.put(ALERTA_2_MESES, "true");
        PADROES.put(ALERTA_3_MESES, "true");
        PADROES.put(ALERTA_VALIDADE_DIAS, "365");
        PADROES.put(APP_TITULO, "OpusSistem — Gestão de Ordens de Serviço");
        PADROES.put(TEMA, "CLARO");
        PADROES.put(COR_DESTAQUE, "#2f6fe4");
        PADROES.put(COR_MENU, "#0d2a63");
    }

    private static final ConfiguracaoService INSTANCIA = new ConfiguracaoService();

    private final ConfiguracaoDAO dao = new ConfiguracaoDAO();
    private Map<String, String> cache;

    public static ConfiguracaoService instancia() {
        return INSTANCIA;
    }

    private synchronized Map<String, String> cache() {
        if (cache == null) {
            cache = new HashMap<>(dao.todas());
        }
        return cache;
    }

    /** Valor salvo, ou o padrão da chave, ou "" se não houver. */
    public synchronized String get(String chave) {
        String v = cache().get(chave);
        if (v == null) {
            v = PADROES.getOrDefault(chave, "");
        }
        return v;
    }

    public boolean getBool(String chave) {
        return Boolean.parseBoolean(get(chave));
    }

    public int getInt(String chave, int padrao) {
        try {
            return Integer.parseInt(get(chave).trim());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    /** Linhas não vazias do valor (listas de sugestões). */
    public List<String> lista(String chave) {
        List<String> itens = new ArrayList<>();
        for (String linha : get(chave).split("\\R")) {
            if (!linha.isBlank()) {
                itens.add(linha.trim());
            }
        }
        return itens;
    }

    public synchronized void salvar(Map<String, String> valores) {
        dao.salvarTodas(valores);
        cache().putAll(valores);
    }

    public void salvar(String chave, String valor) {
        salvar(Map.of(chave, valor == null ? "" : valor));
    }

    /** Recarrega do banco (após importar configurações, por exemplo). */
    public synchronized void recarregar() {
        cache = null;
    }
}
