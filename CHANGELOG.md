# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versionamento [SemVer](https://semver.org/lang/pt-BR/).

## [1.1.0] — 2026-10

### Adicionado
- **Texto de rodapé no relatório**: rodapé por OS, por cliente (ex.: e-mail pedido por um cliente) ou geral,
  centralizado no pé da folha final, abaixo das assinaturas. Migração `V003`.
- Aba **Perfil → Rodapés por cliente** e card "Texto no rodapé da folha" na tela Relatórios.
- **Prévia do relatório** dentro do programa (mesmo desenho da folha A4).
- Campo "Rodapé do relatório" na importação e exportação (CSV/JSON).
- Estrutura de projeto de código aberto: licença MIT, CI no GitHub Actions, modelos de issue/PR, guia de contribuição,
  política de segurança e documentação.

### Alterado
- **Novo visual**, menu lateral com ícones e contadores, cartões de indicador, gráficos próprios,
  tabelas, selos, interruptores, passos da assinatura em lote e abas em botões.
- `pom.xml`: verificação de JDK 26/Maven 3.9 e metadados de licença.

## [1.0.0] — 2026-10

### Adicionado
- Primeira versão desktop (JavaFX + MySQL + JDBC): login e cadastro, dashboard, OS, histórico, relatório em PDF,
  assinatura individual e em lote, alertas de preventivas, exportar/importar e perfil.
