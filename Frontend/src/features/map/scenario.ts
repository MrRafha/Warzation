import type { ApiClient } from "../../lib/apiClient";
import type { GeometryCollection, Scenario, ScenarioData, ScenarioResponse } from "./types";

export const DEFAULT_SCENARIO_ID = "nordeste-teste";

/** Indexa o cenário e confere se dados e geometrias batem. */
export function buildScenario(
  data: ScenarioData,
  geometry: GeometryCollection,
  buildingSlots: Record<string, number>,
): Scenario {
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
    buildingSlots: new Map(Object.entries(buildingSlots)),
  };
}

/** Busca o cenário no Backend (`GET /api/v1/scenarios/{id}`). */
export async function loadScenarioFromApi(api: ApiClient, scenarioId = DEFAULT_SCENARIO_ID): Promise<Scenario> {
  const response = await api.get<ScenarioResponse>(`/api/v1/scenarios/${encodeURIComponent(scenarioId)}`, {
    timeoutMs: 15_000,
  });
  return buildScenario(response.scenario, response.geometry, response.buildingSlots);
}
