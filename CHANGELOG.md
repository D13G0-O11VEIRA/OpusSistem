# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versionamento [SemVer](https://semver.org/lang/pt-BR/).

## [1.2.0] — 2026-10

### Adicionado
- **Ícone do aplicativo** com a logo do OpusSistem (janela, barra de tarefas e diálogos), além de `packaging/icons/`
  com `.ico`, `.icns` e `.png` e instruções de `jpackage`.
- **Tamanho da logo ajustável** por barras deslizantes (menu lateral e relatório) em
  *Perfil → Empresa e relatório → Logo da empresa*, com prévia ao vivo.
- Botão **Assinar como técnico/cliente** agora sempre funciona: se a OS ainda estiver aberta, o programa pergunta se deve
  fechá-la e segue para a janela de assinatura.

### Alterado
- **Sistema sem logo de empresa por padrão.** A logo da empresa passou a ficar no banco (configuração `empresa.logo`)
  em vez do arquivo fixo `storage/logo.png`; só aparece no menu lateral e nos relatórios depois de enviada no Perfil.
  Uma logo antiga em `storage/logo.png` é ignorada (pode ser apagada) e deve ser reenviada pelo Perfil.
- A logo do **OpusSistem** aparece apenas dentro do programa (login, menu e janelas); nunca nos relatórios.

### Corrigido
- Logo de teste de uma instalação anterior aparecendo na primeira inicialização de um banco novo.
- Botões de assinatura da tela Relatórios ficavam desativados, sem explicação, em OS abertas.

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
- **Novo visual**, igual ao da versão web: menu lateral com ícones e contadores, cartões de indicador, gráficos próprios,
  tabelas, selos, interruptores, passos da assinatura em lote e abas em botões.
- `pom.xml`: verificação de JDK 26/Maven 3.9 e metadados de licença.

## [1.0.0] — 2026-10

### Adicionado
- Primeira versão desktop (JavaFX + MySQL + JDBC): login e cadastro, dashboard, OS, histórico, relatório em PDF,
  assinatura individual e em lote, alertas de preventivas, exportar/importar e perfil.
