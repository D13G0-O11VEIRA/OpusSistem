-- V003 - Texto no rodapé da folha final do relatório
--  * rodape_relatorio: rodapé específico de uma OS (tem prioridade sobre o do cliente)
--  * rodapes_cliente: rodapé que um cliente específico pede (ex.: um e-mail no pé da folha)
-- Se nenhum dos dois existir, vale o rodapé geral definido em Perfil > Empresa e relatório.

ALTER TABLE ordens_servico
    ADD COLUMN rodape_relatorio TEXT NULL AFTER observacoes;

CREATE TABLE IF NOT EXISTS rodapes_cliente (
    cliente VARCHAR(150) NOT NULL PRIMARY KEY,
    texto TEXT NOT NULL,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);
