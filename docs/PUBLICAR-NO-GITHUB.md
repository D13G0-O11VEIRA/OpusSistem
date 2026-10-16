# Publicando no GitHub com segurança

Este projeto já vem preparado: o `.gitignore` bloqueia senhas, dados e arquivos gerados. Mesmo assim, siga este
roteiro **antes do primeiro `git push`**.

## 1. O que NÃO pode ir para o repositório

| Arquivo/pasta | Por quê |
|---|---|
| `config/application.properties` | contém a senha do MySQL |
| `storage/` (assinaturas, `relatorios/`, `ordens/`, `logo.png`) | dados e documentos de clientes |
| `target/`, `out/` | arquivos compilados |
| `.env`, `*.pem`, `*.key` | segredos |

Só `config/application.example.properties` (com senha de mentira) deve ser publicado.

## 2. Se alguma senha já apareceu em prints, chats ou arquivos

**Troque-a agora** no MySQL. Remover o arquivo do repositório não apaga o histórico do Git.

## 3. Primeiro commit

```bash
git init
git add .
git status
```

Leia a lista de `git status`. Ela **não** pode conter `application.properties`, `storage/...`, `target/` nem PDFs.
Para confirmar que o Git está ignorando o arquivo de senha:

```bash
git check-ignore -v config/application.properties
```

Procure por senhas esquecidas (Linux/macOS):

```bash
git grep -n -i -E "password|senha|secret|token" -- . ":!config/application.example.properties" ":!*.md" ":!src/test"
```

No Windows (PowerShell):

```powershell
Get-ChildItem -Recurse -File -Include *.java,*.xml,*.properties,*.sql,*.yml |
  Where-Object { $_.FullName -notmatch "target|\.git|example" } |
  Select-String -Pattern "password|senha|secret|token"
```

(As ocorrências em código, como o nome do campo `senha`, são normais; procure por **valores** reais.)

```bash
git commit -m "Primeira versão"
git branch -M main
git remote add origin https://github.com/SEU-USUARIO/OpusSistem.git
git push -u origin main
```

## 4. Depois de publicar

- Em **Settings → Code security**: ative *Private vulnerability reporting*, *Dependabot alerts* e *Secret scanning*.
- Em **Settings → Branches**: proteja `main` (exigir *pull request* e a CI verde).
- Edite o `LICENSE` se quiser outro titular de direitos autorais (hoje: "Contribuidores do OpusSistem").
- Se for colocar prints no README, use **dados fictícios**.

## 5. Distribuindo o programa

`mvn -Pdist package` gera `target/opussistem-dist.jar`. O JAR inclui as bibliotecas nativas do JavaFX **da plataforma
em que foi gerado**; para Windows, Linux e macOS gere em cada sistema (a CI já faz isso e publica os JARs como
*artifacts*). Nunca inclua `config/application.properties` no que você distribui: cada instalação cria o seu.
