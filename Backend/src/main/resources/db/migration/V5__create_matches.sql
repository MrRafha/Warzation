-- Partidas criadas a partir de um cenário, seus participantes e os comandos
-- (idempotentes) que as criaram ou alteraram.
CREATE TABLE matches (
    id VARCHAR(36) PRIMARY KEY,
    scenario_id VARCHAR(50) NOT NULL REFERENCES scenarios (id),
    name VARCHAR(60) NOT NULL,
    status VARCHAR(20) NOT NULL,
    max_players INT NOT NULL,
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_matches_status ON matches (status, created_at);

-- Uma nação por jogador e um jogador por nação em cada partida.
CREATE TABLE match_participants (
    match_id VARCHAR(36) NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    player_id VARCHAR(50) NOT NULL,
    scenario_id VARCHAR(50) NOT NULL,
    nation_id VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (match_id, player_id),
    UNIQUE (match_id, nation_id),
    FOREIGN KEY (scenario_id, nation_id) REFERENCES scenario_nations (scenario_id, id)
);

CREATE TABLE match_commands (
    command_id UUID PRIMARY KEY,
    player_id VARCHAR(50) NOT NULL,
    command_type VARCHAR(30) NOT NULL,
    match_id VARCHAR(36) NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    nation_id VARCHAR(50),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
