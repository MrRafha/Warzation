-- Estado territorial da partida, criado quando ela começa.
ALTER TABLE matches ADD COLUMN started_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE match_provinces (
    match_id VARCHAR(36) NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    province_id VARCHAR(50) NOT NULL,
    scenario_id VARCHAR(50) NOT NULL,
    owner_nation_id VARCHAR(50),
    PRIMARY KEY (match_id, province_id),
    FOREIGN KEY (scenario_id, province_id) REFERENCES scenario_provinces (scenario_id, id),
    FOREIGN KEY (scenario_id, owner_nation_id) REFERENCES scenario_nations (scenario_id, id)
);

-- Pilha de unidades de um tipo numa província; dono nulo = guarnição neutra.
CREATE TABLE match_units (
    id UUID PRIMARY KEY,
    match_id VARCHAR(36) NOT NULL,
    province_id VARCHAR(50) NOT NULL,
    owner_nation_id VARCHAR(50),
    unit_type VARCHAR(30) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    FOREIGN KEY (match_id, province_id) REFERENCES match_provinces (match_id, province_id) ON DELETE CASCADE
);

CREATE INDEX idx_match_units_match ON match_units (match_id);

-- Estoque local por província (Docs/01, seção de logística).
CREATE TABLE match_province_stocks (
    match_id VARCHAR(36) NOT NULL,
    province_id VARCHAR(50) NOT NULL,
    resource VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount >= 0),
    PRIMARY KEY (match_id, province_id, resource),
    FOREIGN KEY (match_id, province_id) REFERENCES match_provinces (match_id, province_id) ON DELETE CASCADE
);
