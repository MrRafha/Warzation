import type { GeometryCollection, Scenario, ScenarioData } from "./types";

/** Indexa o cenário e confere se dados e geometrias batem. */
export function buildScenario(data: ScenarioData, geometry: GeometryCollection): Scenario {
  const provinces = new Map(data.provinces.map((province) => [province.id, province]));
  const geometries = new Map(geometry.features.map((feature) => [feature.properties.id, feature.geometry]));

  const missing = data.provinces.filter((province) => !geometries.has(province.id)).map((province) => province.id);
  if (missing.length > 0) {
    throw new Error(`Província sem geometria: ${missing.join(", ")}`);
  }

  const neighbours = new Map<string, string[]>(data.provinces.map((province) => [province.id, []]));
  for (const [a, b] of data.connections) {
    neighbours.get(a)?.push(b);
    neighbours.get(b)?.push(a);
  }
  for (const list of neighbours.values()) {
    list.sort((a, b) => (provinces.get(a)?.name ?? a).localeCompare(provinces.get(b)?.name ?? b, "pt-BR"));
  }

  return {
    data,
    geometries,
    provinces,
    neighbours,
    stateNames: new Map(data.states.map((state) => [state.id, state.name])),
    terrainNames: new Map(data.terrains.map((terrain) => [terrain.id, terrain.name])),
    capitals: new Set(data.nations.map((nation) => nation.capital)),
  };
}

/**
 * Cenário de teste embutido no build. Provisório: será trocado pela leitura de
 * `/api/v1/...` quando a API do mapa (#15) existir.
 */
export async function loadBundledScenario(): Promise<Scenario> {
  const [{ default: data }, { default: geometryText }] = await Promise.all([
    import("@scenarios/nordeste-teste/scenario.json"),
    import("@scenarios/nordeste-teste/provinces.geojson?raw"),
  ]);
  return buildScenario(data as unknown as ScenarioData, JSON.parse(geometryText) as GeometryCollection);
}
