-- Histórico da partida: um evento por mudança de estado, gravado na mesma
-- transação da mudança. seq é a ordem dentro da partida.
CREATE TABLE match_events (
    id UUID PRIMARY KEY,
    match_id VARCHAR(36) NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    seq INT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    player_id VARCHAR(50),
    command_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    data VARCHAR(4000) NOT NULL,
    UNIQUE (match_id, seq)
);
