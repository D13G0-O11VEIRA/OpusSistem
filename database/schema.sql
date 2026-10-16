-- OpusSistem - esquema consolidado (V001 + V002 + V003)
-- Referência para criação manual. O aplicativo aplica as migrações sozinho
-- (pasta database/migrations), então rodar este arquivo é opcional.

CREATE DATABASE IF NOT EXISTS opussistem
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE opussistem;

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

-- V002 - Campos novos da OS, usuários, configurações, histórico e assinaturas

-- Campos novos na tabela de ordens de serviço
ALTER TABLE ordens_servico
    ADD COLUMN modelo VARCHAR(100) NULL AFTER equipamento,
    ADD COLUMN numero_serie VARCHAR(80) NULL AFTER numero_patrimonio,
    ADD COLUMN status_equipamento VARCHAR(50) NULL AFTER numero_serie,
    ADD COLUMN proxima_preventiva DATE NULL AFTER data_entrega;

ALTER TABLE ordens_servico
    ADD INDEX idx_os_status (status),
    ADD INDEX idx_os_cliente (cliente),
    ADD INDEX idx_os_patrimonio (numero_patrimonio),
    ADD INDEX idx_os_serie (numero_serie),
    ADD INDEX idx_os_entrada (data_entrada),
    ADD INDEX idx_os_preventiva (proxima_preventiva);

-- Usuários do sistema (a senha é guardada como hash PBKDF2)
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    usuario VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(150),
    cargo VARCHAR(100),
    perfil VARCHAR(20) NOT NULL DEFAULT 'OPERADOR',
    senha_hash VARCHAR(255) NOT NULL,
    assinatura_arquivo VARCHAR(255),
    ativo TINYINT(1) NOT NULL DEFAULT 1,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Configurações (empresa, relatório, padrões, listas, alertas, aparência)
CREATE TABLE IF NOT EXISTS configuracoes (
    chave VARCHAR(100) PRIMARY KEY,
    valor MEDIUMTEXT,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

-- Linha do tempo de cada OS
CREATE TABLE IF NOT EXISTS historico_os (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    os_id BIGINT NOT NULL,
    data_hora DATETIME NOT NULL,
    categoria VARCHAR(20) NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT,
    responsavel VARCHAR(150),
    CONSTRAINT fk_historico_os FOREIGN KEY (os_id)
        REFERENCES ordens_servico (id) ON DELETE CASCADE,
    INDEX idx_historico_os (os_id, data_hora)
);

-- Assinaturas digitais (imagem salva em storage/assinaturas)
CREATE TABLE IF NOT EXISTS assinaturas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    os_id BIGINT NOT NULL,
    papel VARCHAR(10) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    arquivo VARCHAR(255) NOT NULL,
    codigo_verificacao VARCHAR(20) NOT NULL,
    pacote_codigo VARCHAR(30),
    assinado_em DATETIME NOT NULL,
    CONSTRAINT fk_assinatura_os FOREIGN KEY (os_id)
        REFERENCES ordens_servico (id) ON DELETE CASCADE,
    UNIQUE KEY uk_assinatura_os_papel (os_id, papel)
);

-- Pacotes de assinatura em lote
CREATE TABLE IF NOT EXISTS pacotes_assinatura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    cliente VARCHAR(150) NOT NULL,
    assinante VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    quantidade INT NOT NULL,
    assinado_em DATETIME NOT NULL
);

-- Numeração das OS (OS-AAAA-0001)
CREATE TABLE IF NOT EXISTS sequencias (
    nome VARCHAR(30) NOT NULL,
    ano INT NOT NULL,
    ultimo INT NOT NULL,
    PRIMARY KEY (nome, ano)
);

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
