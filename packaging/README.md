# Empacotamento e ícone do aplicativo

O ícone do OpusSistem (monograma branco sobre fundo azul) está em `packaging/icons/`:

| Arquivo | Uso |
|---|---|
| `opussistem.ico` | Windows (instalador/atalho; 16 a 256 px) |
| `opussistem.icns` | macOS |
| `opussistem.png` | Linux e uso geral (512 px) |

Os PNGs usados pelo programa (ícone da janela e da barra de tarefas) ficam em `src/main/resources/icons/` e são
carregados por `IconeApp`. O arquivo `monograma-branco.png` (traço branco, fundo transparente) é usado no topo do menu
lateral, que é azul-escuro.

> A logo do OpusSistem aparece **só dentro do programa** (janela, login e menu). Ela nunca sai nos relatórios: a logo
> dos relatórios é a da empresa do usuário, enviada em *Perfil → Empresa e relatório*.

## Gerando um instalador com `jpackage` (opcional)

1. Gere o JAR executável: `mvn -Pdist package` (o JAR leva o JavaFX da plataforma em que foi gerado).
2. Rode o `jpackage` do JDK 26 **no mesmo sistema operacional** do instalador desejado.

Windows (PowerShell):

```powershell
jpackage --name OpusSistem --app-version 1.2.0 --vendor "OpusSistem" `
  --input target --main-jar opussistem-dist.jar --main-class br.opussistem.Main `
  --icon packaging\icons\opussistem.ico `
  --java-options "--enable-native-access=ALL-UNNAMED" `
  --type app-image
```

Linux:

```bash
jpackage --name OpusSistem --app-version 1.2.0 \
  --input target --main-jar opussistem-dist.jar --main-class br.opussistem.Main \
  --icon packaging/icons/opussistem.png \
  --java-options "--enable-native-access=ALL-UNNAMED" --type app-image
```

macOS: troque `--icon` por `packaging/icons/opussistem.icns`.

Use `--type exe`/`msi` (Windows), `deb`/`rpm` (Linux) ou `dmg` (macOS) para instaladores completos (podem exigir
ferramentas extras, como WiX no Windows). Cada instalação cria o seu `config/application.properties`; **nunca** inclua
o seu no pacote.
