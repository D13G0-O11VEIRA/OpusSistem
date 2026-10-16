# Arquitetura

```
view  ──►  controller  ──►  service  ──►  dao  ──►  MySQL
(JavaFX)   (Contexto,       (regras de    (SQL/JDBC)
           Navegador)       negócio, PDF)
```

| Pacote | Responsabilidade |
|---|---|
| `view` | Telas JavaFX (uma classe por tela) e `view/componentes` (`Ui`, `Tabela`, ícones, gráficos, interruptor...) |
| `controller` | `Contexto` (serviços e usuário logado), `Navegador` e `Rota` (troca de telas) |
| `service` | Regras de negócio: OS, alertas, assinaturas, rodapés, relatório (PDF e prévia HTML), importação/exportação |
| `dao` | Acesso ao banco com JDBC puro; um DAO por tabela; `Transacao` cuida de conexão e commit |
| `model` | Entidades, enums e *records* |
| `util` | Conexão, migrações, senhas (PBKDF2), CSV, HTML, formatos, quebra de texto |
| `config` | Leitura de `config/application.properties` (e variáveis `OPUS_*`) |

Regras: a camada de cima só conhece a de baixo; SQL só nos DAOs; regra de negócio só nos serviços; erros esperados usam
`AppException` (mensagem para o usuário) e falhas de banco viram `DaoException`.

## Banco de dados

- As tabelas são criadas e atualizadas por **migrações** em `database/migrations` (lista em `migrations.txt`).
  O `MigradorBanco` cria o banco, registra o que já foi aplicado em `schema_migrations` e executa o que falta.
- Tabelas: `ordens_servico`, `historico_os`, `assinaturas`, `pacotes_assinatura`, `usuarios`, `configuracoes`,
  `rodapes_cliente`, `sequencias`.
- A collation `utf8mb4_0900_ai_ci` ignora acentos e maiúsculas nas buscas.

## Relatório

- **PDF** (OpenPDF): `RelatorioService`. Natureza e laudo vêm do editor de texto (HTML) e são convertidos por
  `ConversorHtmlPdf` (negrito, itálico, sublinhado, cores, listas, alinhamento).
- **Prévia** na tela (WebView): `RelatorioHtml`, com o mesmo desenho da folha. JavaScript fica desligado.
- A **observação** da OS nunca é impressa.
- As **assinaturas** ficam num bloco reservado no pé da página; o **rodapé** (OS > cliente > geral) é desenhado
  encostado na margem inferior, abaixo das assinaturas (`RodapeService`, `Quebra`).
- O **código de verificação** (`Integridade`) é gravado ao assinar; se o conteúdo mudar depois, o relatório avisa.

## Interface

- Estilos em `src/main/resources/css/app.css`, com "cores procuradas" (`-cor-destaque`, `-cor-menu`...). O tema escuro e
  as cores personalizadas só trocam esses valores (`view/componentes/Tema`).
- Ícones são caminhos SVG (`Icones`); gráficos são desenhados com formas JavaFX (`GraficoBarras`, `GraficoRosca`).
- `Main` não estende `Application` de propósito: assim o JavaFX roda pelo classpath (VS Code e JAR único).

## Testes

`mvn verify` roda os testes JUnit 5 de regras puras (CSV, HTML, datas, senhas, alertas, importação, prioridade do
rodapé, quebra de texto). Eles não exigem MySQL nem interface gráfica.
