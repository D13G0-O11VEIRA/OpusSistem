-- Cria um usuário próprio para o OpusSistem (recomendado no lugar do root).
-- Rode como administrador do MySQL e TROQUE a senha antes de executar.

CREATE DATABASE IF NOT EXISTS opussistem
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'opus'@'localhost' IDENTIFIED BY 'TROQUE_ESTA_SENHA';
GRANT ALL PRIVILEGES ON opussistem.* TO 'opus'@'localhost';
FLUSH PRIVILEGES;
