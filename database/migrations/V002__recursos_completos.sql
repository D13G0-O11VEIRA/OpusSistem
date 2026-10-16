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
