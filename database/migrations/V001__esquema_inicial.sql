-- V001 - Esquema inicial (tabela de ordens de serviço)
-- Mantém exatamente o esboço original do projeto.

CREATE TABLE IF NOT EXISTS ordens_servico (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    numero_os VARCHAR(20) NOT NULL UNIQUE,

    cliente VARCHAR(150) NOT NULL,
    telefone VARCHAR(30),

    equipamento VARCHAR(150) NOT NULL,
    fabricante VARCHAR(100),
    numero_patrimonio VARCHAR(50),

    tipo_manutencao VARCHAR(50),
    tecnico_responsavel VARCHAR(150),

    valor_previsto DECIMAL(10,2),
    valor_final DECIMAL(10,2),

    data_entrada DATETIME NOT NULL,
    previsao_entrega DATETIME,
    data_entrega DATETIME,

    status VARCHAR(30) NOT NULL DEFAULT 'ABERTA',

    descricao_problema TEXT,
    laudo_tecnico TEXT,
    observacoes TEXT,

    assinatura_cliente VARCHAR(255),

    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);
