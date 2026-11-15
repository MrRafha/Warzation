-- Sala de teste da Sprint 1: estado mínimo e oficial de uma partida.
CREATE TABLE rooms (
    id VARCHAR(50) PRIMARY KEY,
    counter_value BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Comandos já aplicados. A chave é o commandId enviado pelo cliente, o que
-- impede reaplicar o mesmo comando, inclusive depois de reiniciar o servidor.
CREATE TABLE processed_commands (
    command_id UUID PRIMARY KEY,
    room_id VARCHAR(50) NOT NULL REFERENCES rooms (id),
    command_type VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    resulting_counter BIGINT NOT NULL,
    resulting_version BIGINT NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_commands_room ON processed_commands (room_id, processed_at);

INSERT INTO rooms (id) VALUES ('dev');
