package br.opussistem.service;

import br.opussistem.model.Assinatura;
import br.opussistem.model.OrdemServico;
import br.opussistem.model.PapelAssinatura;
import br.opussistem.util.Formatos;
import br.opussistem.util.Html;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Prévia do relatório em HTML (exibida dentro do programa, com o mesmo desenho da folha A4 do PDF):
 * cabeçalho, dados da OS, natureza e laudo com títulos centralizados, assinaturas e texto de rodapé no pé da
 * folha. A observação da OS não aparece, assim como no PDF.
 */
public class RelatorioHtml {

    private static final Safelist PERMITIDO = Safelist.relaxed()
            .addTags("font", "s", "strike")
            .addAttributes(":all", "style", "align", "color", "size");

    private static final String CSS = """
            body{margin:12px;background:#e9edf3;font-family:Arial,Helvetica,sans-serif;font-size:12px;color:#111}
            .pg{width:595px;min-height:842px;box-sizing:border-box;margin:0 auto 14px;background:#fff;padding:34px 36px;
              display:-webkit-box;display:-webkit-flex;display:flex;-webkit-flex-direction:column;flex-direction:column;
              box-shadow:0 2px 10px rgba(0,0,0,.20)}
            .rh{display:-webkit-box;display:flex;align-items:center;padding-bottom:8px;border-bottom:1px solid #999}
            .rh img{margin-right:12px}
            h2{text-align:center;font-size:17px;margin:14px 0}
            h4{text-align:center;font-size:13px;margin:12px 0 4px}
            table.in{width:100%;border-collapse:collapse}
            table.in td{border:1px solid #777;padding:4px 6px;width:33.3%;font-size:11px}
            .bx{border:1px solid #777;padding:6px 8px;margin:0 0 6px;word-break:break-word}
            .bx ul,.bx ol{margin:4px 0;padding-left:20px}
            table.lista{width:100%;border-collapse:collapse;font-size:11px}
            table.lista th,table.lista td{border:1px solid #999;padding:3px 6px;text-align:left}
            table.lista th{background:#ebeef5}
            .rf{margin-top:auto;padding-top:24px}
            .sg{display:-webkit-box;display:flex;justify-content:space-around}
            .sc{width:44%;text-align:center}
            .sc img{max-height:54px;max-width:100%;display:block;margin:0 auto -6px}
            .sc .gap{height:54px}
            .sc .linha{border-top:1px solid #111;padding-top:3px}
            .sc small{display:block;color:#444;font-size:10px}
            .vf{text-align:center;color:#555;font-size:10px;margin:6px 0 0}
            .val{text-align:center;font-weight:bold;margin:14px 0 0}
            .ft{margin-top:10px;padding-top:6px;border-top:1px solid #aaa;text-align:center;color:#555;font-size:10px;
              white-space:pre-wrap}
            .aviso{color:#b00020;font-weight:bold}
            """;

    private final AssinaturaService assinaturas;
    private final RodapeService rodapes;
    private final LogoService logos;
    private final ConfiguracaoService config = ConfiguracaoService.instancia();

    public RelatorioHtml(AssinaturaService assinaturas, RodapeService rodapes, LogoService logos) {
        this.assinaturas = assinaturas;
        this.rodapes = rodapes;
        this.logos = logos;
    }

    /** Documento completo de uma OS (uma folha). */
    public String paginaDaOS(OrdemServico os) {
        return documento(corpoDaOS(os));
    }

    /** Capa do pacote de assinatura em lote. */
    public String capa(String cliente, List<OrdemServico> lista) {
        return documento(corpoDaCapa(cliente, lista));
    }

    public String documento(String corpo) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>" + CSS + "</style></head><body>"
                + corpo + "</body></html>";
    }

    // ------------------------------------------------------------------ folhas

    private String corpoDaOS(OrdemServico os) {
        StringBuilder h = new StringBuilder("<div class=\"pg\">");
        h.append(cabecalho());
        h.append("<h2>").append(Html.escapar(config.get(ConfiguracaoService.REL_TITULO))).append("</h2>");
        h.append(tabelaDeDados(os));
        h.append("<h4>").append(Html.escapar(Rotulos.de("natureza"))).append("</h4><div class=\"bx\">")
                .append(rico(os.getDescricaoProblema())).append("</div>");
        h.append("<h4>").append(Html.escapar(Rotulos.de("laudo"))).append("</h4><div class=\"bx\">")
                .append(rico(os.getLaudoTecnico())).append("</div>");
        if (os.getValorFinal() != null && os.getValorFinal().signum() > 0) {
            h.append("<p><b>").append(Html.escapar(Rotulos.de("valor"))).append(":</b> ")
                    .append(Html.escapar(Formatos.moeda(os.getValorFinal()))).append("</p>");
        }
        Map<PapelAssinatura, Assinatura> ass = assinaturas.daOS(os.getId());
        h.append("<div class=\"rf\">");
        h.append("<div class=\"sg\">")
                .append(assinatura(ass.get(PapelAssinatura.CLIENTE), config.get(ConfiguracaoService.REL_ASSINATURA1), os))
                .append(assinatura(ass.get(PapelAssinatura.TECNICO), config.get(ConfiguracaoService.REL_ASSINATURA2), os))
                .append("</div>");
        int dias = config.getInt(ConfiguracaoService.REL_VALIDADE_DIAS, 0);
        if (dias > 0) {
            LocalDateTime base = os.dataReferencia();
            h.append("<p class=\"val\">").append(Html.escapar(config.get(ConfiguracaoService.REL_VALIDADE_ROTULO)))
                    .append(": ").append(Formatos.data(base.plusDays(dias))).append("</p>");
        }
        Assinatura ref = ass.get(PapelAssinatura.CLIENTE) != null ? ass.get(PapelAssinatura.CLIENTE)
                : ass.get(PapelAssinatura.TECNICO);
        if (ref != null) {
            h.append("<p class=\"vf\">Código de verificação: ").append(Html.escapar(ref.getCodigoVerificacao()))
                    .append("</p>");
        }
        h.append(rodape(rodapes.efetivo(os).texto()));
        h.append("</div></div>");
        return h.toString();
    }

    private String corpoDaCapa(String cliente, List<OrdemServico> lista) {
        StringBuilder h = new StringBuilder("<div class=\"pg\">");
        h.append(cabecalho());
        h.append("<div style=\"text-align:center;margin:26px 0 14px\"><h2 style=\"margin:0 0 10px\">")
                .append("Pacote de Relatórios de Atendimento Técnico</h2><div style=\"font-size:22px;font-weight:bold\">")
                .append(Html.escapar(cliente)).append("</div><p>").append(lista.size())
                .append(lista.size() == 1 ? " ordem de serviço" : " ordens de serviço").append("</p></div>");
        h.append("<table class=\"lista\"><tr><th>Nº OS</th><th>").append(Html.escapar(Rotulos.de("equipamento")))
                .append("</th><th>").append(Html.escapar(Rotulos.de("patrimonio"))).append("</th><th>Data</th><th>")
                .append(Html.escapar(Rotulos.de("tecnico"))).append("</th></tr>");
        Assinatura assinada = null;
        for (OrdemServico os : lista) {
            h.append("<tr><td>").append(Html.escapar(os.getNumero())).append("</td><td>")
                    .append(Html.escapar(Formatos.texto(os.getEquipamento()))).append("</td><td>")
                    .append(Html.escapar(Formatos.texto(os.getPatrimonio()))).append("</td><td>")
                    .append(Formatos.data(os.dataReferencia())).append("</td><td>")
                    .append(Html.escapar(Formatos.texto(os.getTecnico()))).append("</td></tr>");
            if (assinada == null) {
                assinada = assinaturas.daOS(os.getId()).get(PapelAssinatura.CLIENTE);
            }
        }
        h.append("</table><p>Com a assinatura abaixo, o cliente confirma o recebimento e a ciência dos ")
                .append(lista.size()).append(lista.size() == 1 ? " relatório" : " relatórios").append(" deste pacote.</p>");
        h.append("<div class=\"rf\"><div class=\"sg\">")
                .append(assinatura(assinada, config.get(ConfiguracaoService.REL_ASSINATURA1), null))
                .append("</div>").append(rodape(rodapes.efetivoDoCliente(cliente).texto())).append("</div></div>");
        return h.toString();
    }

    // ------------------------------------------------------------------ partes

    private String cabecalho() {
        List<String> linhas = new ArrayList<>();
        adicionar(linhas, "Razão Social", config.get(ConfiguracaoService.EMPRESA_RAZAO), null, null, null, null);
        adicionar(linhas, "Fantasia", config.get(ConfiguracaoService.EMPRESA_FANTASIA), "Telefone",
                config.get(ConfiguracaoService.EMPRESA_TELEFONE), "E-mail", config.get(ConfiguracaoService.EMPRESA_EMAIL));
        adicionar(linhas, "Endereço", config.get(ConfiguracaoService.EMPRESA_ENDERECO), null, null, null, null);
        adicionar(linhas, "CNPJ", config.get(ConfiguracaoService.EMPRESA_CNPJ), "Inscrição Estadual",
                config.get(ConfiguracaoService.EMPRESA_IE), null, null);
        for (String registro : config.get(ConfiguracaoService.EMPRESA_REGISTROS).split("\\R")) {
            if (!registro.isBlank()) {
                linhas.add(Html.escapar(registro.trim()));
            }
        }
        String logo = logoComoDados();
        if (linhas.isEmpty() && logo == null) {
            return "";
        }
        int lado = logos.tamanhoRelatorio(); // 1 pt do PDF = 1 px nesta prévia (folha de 595 px)
        return "<div class=\"rh\">" + (logo == null ? ""
                : "<img src=\"" + logo + "\" alt=\"\" style=\"max-width:" + lado + "px;max-height:" + lado + "px\">")
                + "<div>" + String.join("<br>", linhas) + "</div></div>";
    }

    private static void adicionar(List<String> linhas, String r1, String v1, String r2, String v2, String r3, String v3) {
        StringBuilder f = new StringBuilder();
        String[][] pares = { { r1, v1 }, { r2, v2 }, { r3, v3 } };
        for (String[] par : pares) {
            if (par[0] != null && par[1] != null && !par[1].isBlank()) {
                if (f.length() > 0) {
                    f.append(" · ");
                }
                f.append("<b>").append(Html.escapar(par[0])).append(":</b> ").append(Html.escapar(par[1].trim()));
            }
        }
        if (f.length() > 0) {
            linhas.add(f.toString());
        }
    }

    private String tabelaDeDados(OrdemServico os) {
        List<String[]> pares = new ArrayList<>();
        pares.add(new String[] { "Nº OS", os.getNumero() });
        pares.add(new String[] { Rotulos.de("cliente"), os.getCliente() });
        pares.add(new String[] { "Data", Formatos.data(os.dataReferencia()) });
        pares.add(new String[] { Rotulos.de("equipamento"), os.getEquipamento() });
        pares.add(new String[] { Rotulos.de("fabricante"), os.getFabricante() });
        pares.add(new String[] { Rotulos.de("modelo"), os.getModelo() });
        pares.add(new String[] { Rotulos.de("patrimonio"), os.getPatrimonio() });
        pares.add(new String[] { Rotulos.de("serie"), os.getNumeroSerie() });
        pares.add(new String[] { Rotulos.de("tecnico"), os.getTecnico() });
        StringBuilder h = new StringBuilder("<table class=\"in\"><tr>");
        int n = 0;
        for (String[] par : pares) {
            if (par[1] == null || par[1].isBlank()) {
                continue;
            }
            if (n > 0 && n % 3 == 0) {
                h.append("</tr><tr>");
            }
            h.append("<td><b>").append(Html.escapar(par[0])).append(":</b> ").append(Html.escapar(par[1])).append("</td>");
            n++;
        }
        while (n % 3 != 0) {
            h.append("<td>&nbsp;</td>");
            n++;
        }
        return h.append("</tr></table>").toString();
    }

    private String assinatura(Assinatura a, String rotulo, OrdemServico os) {
        StringBuilder h = new StringBuilder("<div class=\"sc\">");
        String imagem = a == null ? null : assinaturas.imagem(a).map(b -> "data:image/png;base64," + Base64.getEncoder().encodeToString(b))
                .orElse(null);
        h.append(imagem == null ? "<div class=\"gap\"></div>" : "<img src=\"" + imagem + "\" alt=\"\">");
        h.append("<div class=\"linha\">").append(Html.escapar(rotulo)).append("</div>");
        if (a != null) {
            h.append("<small>").append(Html.escapar(a.getNome())).append(" · ").append(Formatos.dataHora(a.getAssinadoEm()))
                    .append("</small>");
            if (os != null && !assinaturas.valida(a, os)) {
                h.append("<small class=\"aviso\">documento alterado após a assinatura</small>");
            }
        }
        return h.append("</div>").toString();
    }

    private static String rodape(String texto) {
        return texto == null || texto.isBlank() ? "" : "<div class=\"ft\">" + Html.escapar(texto) + "</div>";
    }

    /** HTML do editor (limpo) ou texto simples escapado. */
    static String rico(String valor) {
        if (valor == null || valor.isBlank()) {
            return "&nbsp;";
        }
        if (!Html.pareceHtml(valor)) {
            return Html.escapar(valor).replace("\n", "<br>");
        }
        String limpo = Jsoup.clean(Html.corpo(valor), PERMITIDO);
        return limpo.isBlank() ? "&nbsp;" : limpo;
    }

    private String logoComoDados() {
        return logos.imagem().map(b -> "data:image/png;base64," + Base64.getEncoder().encodeToString(b)).orElse(null);
    }
}
