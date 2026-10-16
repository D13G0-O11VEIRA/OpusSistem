# Texto no rodapé da folha final do relatório

Alguns clientes pedem um texto fixo no pé da folha — por exemplo, um **e-mail para contato**. O OpusSistem tem três
níveis de rodapé; vale o primeiro que existir:

1. **Rodapé da OS** — só para aquele relatório.
2. **Rodapé do cliente** — para todos os relatórios desse cliente.
3. **Rodapé geral** — para todos os demais (Perfil → Empresa e relatório).

## Como usar

### Para um relatório (ou para todos os de um cliente)
1. Abra **Relatórios** e escolha a OS.
2. No card **Texto no rodapé da folha**, digite o texto (até 1000 caracteres).
3. Em **Aplicar a**, escolha *Somente esta OS* ou *Todos os relatórios deste cliente*.
4. Clique em **Salvar rodapé**. A prévia atualiza na hora; o PDF sai com o texto no pé da página.

Para remover, apague o texto e salve (ou use **Remover rodapé específico**).

### Gerenciar vários clientes
**Perfil → Rodapés por cliente** lista todos os clientes com rodapé próprio e permite adicionar, editar e excluir.
Use o mesmo nome do cliente que aparece nas OS (maiúsculas e acentos não importam).

### Rodapé geral
**Perfil → Empresa e relatório → Rodapé geral do relatório**.

## Detalhes
- O texto é centralizado, em fonte pequena e cinza, com um fio fino acima; quebras de linha são respeitadas e linhas
  muito longas são quebradas automaticamente (máximo de 8 linhas no PDF).
- Nos **pacotes de assinatura em lote**, cada relatório usa o próprio rodapé e a capa usa o do cliente.
- O rodapé **não** faz parte do conteúdo assinado: mudar o rodapé não invalida assinaturas.
- Importação e exportação (CSV/JSON) incluem a coluna *Rodapé do relatório*.
