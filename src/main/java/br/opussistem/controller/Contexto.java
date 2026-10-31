package br.opussistem.controller;

import br.opussistem.model.Usuario;
import br.opussistem.service.AlertaService;
import br.opussistem.service.AssinaturaService;
import br.opussistem.service.AuthService;
import br.opussistem.service.ConfiguracaoService;
import br.opussistem.service.DashboardService;
import br.opussistem.service.ImportExportService;
import br.opussistem.service.LogoService;
import br.opussistem.service.OrdemServicoService;
import br.opussistem.service.RelatorioHtml;
import br.opussistem.service.RelatorioService;
import br.opussistem.service.RodapeService;

/** Acesso central aos serviços e ao usuário logado (as telas não criam serviços por conta própria). */
public final class Contexto {

    public static final ConfiguracaoService CONFIG = ConfiguracaoService.instancia();
    public static final AuthService AUTH = new AuthService();
    public static final OrdemServicoService OS = new OrdemServicoService();
    public static final AlertaService ALERTAS = new AlertaService();
    public static final DashboardService DASHBOARD = new DashboardService(ALERTAS);
    public static final AssinaturaService ASSINATURAS = new AssinaturaService();
    public static final RodapeService RODAPES = new RodapeService();
    public static final LogoService LOGO = new LogoService();
    public static final RelatorioService RELATORIOS = new RelatorioService(ASSINATURAS, RODAPES, LOGO);
    public static final RelatorioHtml PREVIA = new RelatorioHtml(ASSINATURAS, RODAPES, LOGO);
    public static final ImportExportService IMPORT_EXPORT = new ImportExportService(OS);

    private static Usuario usuario;

    private Contexto() {
    }

    public static Usuario usuario() {
        return usuario;
    }

    public static void entrar(Usuario u) {
        usuario = u;
    }

    public static void sair() {
        usuario = null;
    }
}
