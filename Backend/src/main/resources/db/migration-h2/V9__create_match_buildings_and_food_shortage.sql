-- Construções de uma província na partida (Sprint 3, #33/#34). A produção só
-- considera as concluídas; as ordens de construção chegam na #34.
CREATE TABLE match_buildings (
    id UUID PRIMARY KEY,
    match_id VARCHAR(36) NOT NULL,
    province_id VARCHAR(50) NOT NULL,
    building_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completes_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (match_id, province_id) REFERENCES match_provinces (match_id, province_id) ON DELETE CASCADE
);

CREATE INDEX idx_match_buildings_match ON match_buildings (match_id);

-- Falta de comida: quanto faltou no último ciclo e o total acumulado.
ALTER TABLE match_provinces ADD COLUMN food_shortage_last_cycle BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE match_provinces ADD COLUMN food_shortage_total BIGINT DEFAULT 0 NOT NULL;
