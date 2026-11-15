-- Jogador que enviou cada comando (identificação da Sprint 2). Registros
-- anteriores ficam sem jogador.
ALTER TABLE processed_commands ADD COLUMN player_id VARCHAR(50);
