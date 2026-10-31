# OpusSistem

Sistema de **gestão de ordens de serviço (OS)** para desktop, de código aberto: cadastro e acompanhamento de OS,
histórico completo, relatório de atendimento técnico em PDF, assinatura digital (individual e em lote), alertas de
preventivas e importação/exportação de dados.

> **Stack:** Java 26 · JavaFX 26 · MySQL 8 · JDBC · Maven (+ OpenPDF, jsoup e Gson)

## Recursos

| Área | O que faz |
|---|---|
| **Acesso** | Login e criação de conta na mesma tela; o primeiro usuário vira administrador; senhas com PBKDF2 |
| **OS** | Cadastro, alteração, busca com filtros e paginação, reabertura com motivo, anexos, texto formatado (negrito, listas, cores) |
| **Histórico** | Linha do tempo de cada OS (quem fez o quê e quando) |
| **Relatório em PDF** | Folha A4 com cabeçalho da empresa, natureza do serviço e laudo, assinaturas no rodapé e **texto de rodapé** |
| **Rodapé do relatório** | Texto no pé da folha final, por **OS**, por **cliente** (ex.: o e-mail que um cliente pede) ou geral |
| **Assinatura digital** | Assinatura desenhada do técnico e do cliente, com código de verificação que acusa alteração posterior |
| **Assinatura em lote** | O cliente assina uma vez e a assinatura vale para todos os relatórios do pacote |
| **Alertas de preventivas** | Avisos 3, 2 e 1 mês antes do vencimento, programação de OS preventiva e regras ajustáveis |
| **Exportar / Importar** | CSV e JSON; importa arquivos de outros sistemas com mapeamento automático de colunas |
| **Personalização** | Logo da empresa (opcional, com tamanho ajustável no menu e no relatório), dados da empresa, nomes dos campos (ex.: Patrimônio → Placa), listas, cores e tema claro/escuro |

## Como rodar

### Requisitos
- **JDK 26** · **Maven 3.6.3+** (recomendado 3.9+) · **MySQL 8** em execução

### Passo a passo
1. **Configure a conexão**: copie `config/application.example.properties` para `config/application.properties`
   e preencha usuário e senha do MySQL. Esse arquivo **nunca vai para o Git** (está no `.gitignore`).
2. *(Recomendado)* Em vez do `root`, crie um usuário próprio com `database/criar-usuario.sql` (troque a senha antes).
3. **Execute**
   - Terminal: `mvn javafx:run`
   - VS Code: *Run and Debug* → **OpusSistem** (já configurado em `.vscode/launch.json`)
4. Na primeira execução o programa **cria o banco e as tabelas sozinho** (migrações em `database/migrations`).
   Use a aba **Criar conta** para cadastrar o primeiro usuário (administrador).

### Comandos úteis
| Comando | O que faz |
|---|---|
| `mvn verify` | compila e roda todos os testes (não precisam de MySQL nem de interface gráfica) |
| `mvn -Pdist package` | gera `target/opussistem-dist.jar` (executável com `java -jar`; contém o JavaFX da plataforma atual) |
| `mvn exec:java` | alternativa ao `javafx:run` |

### Logo da empresa e ícone do aplicativo
- O sistema **vem sem logo de empresa**. Ela só aparece (no menu lateral e nos relatórios) depois que o usuário a envia em
  **Perfil → Empresa e relatório → Logo da empresa**, onde duas barras deslizantes ajustam o tamanho no menu e no relatório.
- A logo fica guardada no **banco de dados**, então um banco novo nunca herda a logo de outra instalação.
- A logo e o ícone do **OpusSistem** aparecem só dentro do programa (janela, login e menu). Ícones para instaladores
  estão em [`packaging/`](packaging/README.md).

### Fonte Inter (opcional)
O visual usa a fonte **Inter**. Ela não vem no repositório: sem ela o programa usa Segoe UI/fonte do sistema.
Para ficar idêntico, veja [`src/main/resources/fonts/LEIA-ME.md`](src/main/resources/fonts/LEIA-ME.md).

## Estrutura

```
OpusSistem/
├─ pom.xml
├─ config/                    application.example.properties (copie para application.properties)
├─ database/
│  ├─ migrations/             V001, V002, V003 ... + migrations.txt (ordem de aplicação)
│  ├─ schema.sql              esquema consolidado (referência)
│  └─ criar-usuario.sql       usuário MySQL dedicado
├─ docs/                      arquitetura, rodapé do relatório, como publicar no GitHub, ideias
├─ templates/                 modelo-importacao.csv
├─ packaging/                 ícones (.ico, .icns, .png) e instruções do jpackage
├─ storage/                   gerado em uso: assinaturas, PDFs e anexos (fora do Git)
├─ src/main/java/br/opussistem/
│  ├─ Main.java · App.java    entrada e aplicação JavaFX
│  ├─ config/ model/ dao/ service/ controller/ view/ util/ exception/
├─ src/main/resources/        css/app.css, fonts/
├─ src/test/java/             testes JUnit 5
└─ .github/                   CI, modelos de issue e de pull request
```

Camadas: `view` → `controller` (Contexto/Navegador) → `service` → `dao` → MySQL. Detalhes em
[`docs/ARQUITETURA.md`](docs/ARQUITETURA.md).

## Segurança e privacidade

- A senha do banco **não fica no código**: vem de `config/application.properties` ou de variáveis `OPUS_DB_*`.
- Senhas de usuários usam **PBKDF2-HMAC-SHA256** (600 mil iterações, salt aleatório).
- A pasta `storage/` (assinaturas, PDFs, logo, anexos) e `config/application.properties` são **ignorados pelo Git**.
- A assinatura digital é um registro (imagem + nome + data + código de verificação), **não** um certificado ICP-Brasil.
- Encontrou uma falha? Veja [`SECURITY.md`](SECURITY.md).

## Contribuindo

Contribuições são bem-vindas! Leia [`CONTRIBUTING.md`](CONTRIBUTING.md) e o [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).
Para publicar uma cópia sua no GitHub com segurança, veja [`docs/PUBLICAR-NO-GITHUB.md`](docs/PUBLICAR-NO-GITHUB.md).

## Licença

[MIT](LICENSE). As licenças das bibliotecas usadas estão em [`THIRD_PARTY_LICENSES.md`](THIRD_PARTY_LICENSES.md).
