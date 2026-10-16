# Política de Segurança

## Versões com suporte

| Versão | Suporte |
|---|---|
| 1.1.x | ✅ |
| < 1.1 | ❌ |

## Como relatar uma vulnerabilidade

**Não abra uma issue pública** para falhas de segurança.

Use o recurso **Security → Report a vulnerability** (relato privado) do repositório no GitHub. Se ele não estiver
disponível, contate as pessoas mantenedoras pelos canais do perfil do repositório. Inclua:

- descrição do problema e do impacto;
- passos para reproduzir;
- versão do OpusSistem, do JDK e do MySQL.

**Nunca** envie senhas reais, o arquivo `application.properties` ou dados de clientes. Responderemos o mais rápido
possível e daremos o crédito a quem reportar, se desejar.

## Pontos importantes sobre o modelo de segurança

- O login do OpusSistem protege o **uso do programa**; o acesso direto ao banco MySQL é controlado pelo próprio MySQL.
  Use um usuário MySQL dedicado, com senha forte e permissões só no banco `opussistem`
  (veja `database/criar-usuario.sql`).
- Senhas de usuários são guardadas com PBKDF2-HMAC-SHA256 e salt aleatório.
- A pasta `storage/` guarda assinaturas, PDFs e anexos **sem criptografia**: proteja-a com as permissões do sistema
  operacional e faça backup.
- A assinatura digital é um registro com código de verificação, **sem** validade de certificado ICP-Brasil.

## Se você publicou uma senha sem querer

1. **Troque a senha imediatamente** no MySQL (remover o arquivo do Git não apaga o histórico).
2. Remova o segredo do histórico (por exemplo, com `git filter-repo`) ou crie um repositório novo.
3. Revise se algum dado de cliente também foi publicado.
