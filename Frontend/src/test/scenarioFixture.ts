import data from "@scenarios/nordeste-teste/scenario.json";
import geometryText from "@scenarios/nordeste-teste/provinces.geojson?raw";
import { buildScenario } from "../features/map/scenario";
import type { GeometryCollection, Scenario, ScenarioData, ScenarioResponse } from "../features/map/types";

/**
 * Cenário real de Backend/data como resposta da API, para testes. Os espaços
 * de construção imitam o servidor (4 nas capitais, 3 nas demais).
 */
export function scenarioResponseFixture(): ScenarioResponse {
  const scenario = data as unknown as ScenarioData;
  const capitals = new Set(scenario.nations.map((nation) => nation.capital));
  return {
    scenario,
    geometry: JSON.parse(geometryText) as GeometryCollection,
    buildingSlots: Object.fromEntries(scenario.provinces.map((p) => [p.id, capitals.has(p.id) ? 4 : 3])),
  };
}

export async function loadFixtureScenario(): Promise<Scenario> {
  const response = scenarioResponseFixture();
  return buildScenario(response.scenario, response.geometry, response.buildingSlots);
}
