# Fonte Inter (opcional)

A interface usa a fonte **Inter**, a mesma da versão web. Ela **não** vem no repositório; sem ela o programa usa
"Segoe UI" (Windows) ou outra fonte do sistema, e tudo funciona normalmente.

Para ficar idêntico à versão web:

1. Baixe a Inter em <https://rsms.me/inter/> (ou no Google Fonts). Licença **SIL Open Font License 1.1**.
2. Copie estes arquivos `.ttf` para esta pasta (`src/main/resources/fonts/`):
   - `Inter-Regular.ttf`
   - `Inter-Medium.ttf`
   - `Inter-SemiBold.ttf`
   - `Inter-Bold.ttf`
3. Rode o programa de novo. O carregamento é automático (`Fontes.carregar()`).

Se você redistribuir o programa com a fonte, inclua o texto da licença OFL junto com ela.
