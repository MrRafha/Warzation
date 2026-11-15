-- Relógio econômico da partida (Sprint 3, #32).
-- next_economy_cycle_at: quando vence o próximo ciclo; nulo até a partida começar.
-- economy_cycles: ciclos aplicados; economy_cycles_skipped: ciclos descartados
-- por excederem a recuperação máxima após o servidor ficar desligado.
ALTER TABLE matches ADD COLUMN next_economy_cycle_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE matches ADD COLUMN economy_cycles BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE matches ADD COLUMN economy_cycles_skipped BIGINT DEFAULT 0 NOT NULL;

CREATE INDEX idx_matches_economy_due ON matches (status, next_economy_cycle_at);
