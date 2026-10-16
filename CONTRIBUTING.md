# Contribuindo com o OpusSistem

Obrigado pelo interesse! Este guia explica como preparar o ambiente e enviar uma contribuição.

## Preparando o ambiente

1. Instale o **JDK 26**, o **Maven 3.6.3+** (recomendado 3.9+) e o **MySQL 8**.
2. Faça um *fork* e clone o repositório.
3. Copie `config/application.example.properties` para `config/application.properties` e ajuste o acesso ao MySQL.
   (Use um banco de **testes**; o programa cria o banco e as tabelas sozinho.)
4. `mvn verify` deve passar antes de você começar a mexer.
5. `mvn javafx:run` abre o programa.

## Regras de ouro

- **Nunca** versione senhas, `application.properties`, PDFs, logos, assinaturas ou dados de clientes.
  O `.gitignore` já cobre esses arquivos; confira com `git status` antes de cada commit.
- Mudanças no banco entram como **nova migração** (`database/migrations/V00X__descricao.sql`) e uma linha em
  `database/migrations/migrations.txt`. Não edite migrações já publicadas.
- Regra de negócio vai em `service/`, SQL em `dao/`, telas em `view/`. Telas não acessam o banco diretamente.
- Mudou regra de negócio? Adicione ou ajuste um teste em `src/test/java`.

## Estilo

- Java com 4 espaços, UTF-8 (veja `.editorconfig`). Nomes de classes e métodos em português, como no restante do projeto.
- Mensagens ao usuário em português, claras e sem jargão. Erros esperados usam `AppException`.
- Prefira classes pequenas e métodos curtos; comente o *porquê*, não o *quê*.

## Fluxo de trabalho

1. Crie uma branch a partir de `main` (`feature/rodape-por-cliente`, `fix/pdf-acentos`...).
2. Faça commits pequenos, com mensagens no imperativo (`Adiciona rodapé por cliente`).
3. Rode `mvn verify`.
4. Abra um *pull request* preenchendo o modelo. A CI roda em Windows, Linux e macOS.

## Relatando problemas

Use os modelos de *issue*. **Não** cole dados sensíveis em issues; anonimize prints e logs.
Falhas de segurança: veja [`SECURITY.md`](SECURITY.md).
