-- Definição dos cenários (mapas), carregada de data/scenarios no startup.
-- Partidas futuras referenciam províncias e nações por estas chaves.
CREATE TABLE scenarios (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    format_version INT NOT NULL,
    provisional BOOLEAN NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    loaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE scenario_provinces (
    scenario_id VARCHAR(50) NOT NULL REFERENCES scenarios (id) ON DELETE CASCADE,
    id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    state_id VARCHAR(10) NOT NULL,
    main_city VARCHAR(100) NOT NULL,
    city_lon DOUBLE PRECISION NOT NULL,
    city_lat DOUBLE PRECISION NOT NULL,
    terrain VARCHAR(50) NOT NULL,
    coastal BOOLEAN NOT NULL,
    building_slots INT NOT NULL,
    PRIMARY KEY (scenario_id, id)
);

CREATE TABLE scenario_province_deposits (
    scenario_id VARCHAR(50) NOT NULL,
    province_id VARCHAR(50) NOT NULL,
    resource VARCHAR(20) NOT NULL,
    abundance VARCHAR(10) NOT NULL,
    PRIMARY KEY (scenario_id, province_id, resource),
    FOREIGN KEY (scenario_id, province_id) REFERENCES scenario_provinces (scenario_id, id) ON DELETE CASCADE
);

CREATE TABLE scenario_connections (
    scenario_id VARCHAR(50) NOT NULL,
    province_a VARCHAR(50) NOT NULL,
    province_b VARCHAR(50) NOT NULL,
    PRIMARY KEY (scenario_id, province_a, province_b),
    FOREIGN KEY (scenario_id, province_a) REFERENCES scenario_provinces (scenario_id, id) ON DELETE CASCADE,
    FOREIGN KEY (scenario_id, province_b) REFERENCES scenario_provinces (scenario_id, id) ON DELETE CASCADE
);

CREATE TABLE scenario_nations (
    scenario_id VARCHAR(50) NOT NULL REFERENCES scenarios (id) ON DELETE CASCADE,
    id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    capital_province_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (scenario_id, id),
    FOREIGN KEY (scenario_id, capital_province_id) REFERENCES scenario_provinces (scenario_id, id) ON DELETE CASCADE
);

CREATE TABLE scenario_nation_provinces (
    scenario_id VARCHAR(50) NOT NULL,
    nation_id VARCHAR(50) NOT NULL,
    province_id VARCHAR(50) NOT NULL,
    PRIMARY KEY (scenario_id, nation_id, province_id),
    UNIQUE (scenario_id, province_id),
    FOREIGN KEY (scenario_id, nation_id) REFERENCES scenario_nations (scenario_id, id) ON DELETE CASCADE,
    FOREIGN KEY (scenario_id, province_id) REFERENCES scenario_provinces (scenario_id, id) ON DELETE CASCADE
);
