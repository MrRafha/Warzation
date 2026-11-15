import { describe, expect, it } from "vitest";
import { vi } from "vitest";
import { loadFixtureScenario, scenarioResponseFixture } from "../../test/scenarioFixture";
import { buildScenario, loadScenarioFromApi } from "./scenario";
import type { GeometryCollection, ScenarioData } from "./types";

describe("cenário nordeste-teste", () => {
  it("carrega o cenário do Nordeste com geometria para todas as províncias", async () => {
    const scenario = await loadFixtureScenario();

    expect(scenario.data.id).toBe("nordeste-teste");
    expect(scenario.provinces.size).toBe(24);
    expect(scenario.geometries.size).toBe(24);
    expect(scenario.capitals).toEqual(new Set(["pi-teresina", "ba-salvador"]));
  });

  it("monta vizinhanças simétricas a partir das conexões", async () => {
    const scenario = await loadFixtureScenario();

    for (const [id, neighbours] of scenario.neighbours) {
      for (const neighbour of neighbours) {
        expect(scenario.neighbours.get(neighbour)).toContain(id);
      }
    }
    expect(scenario.neighbours.get("pi-teresina")).toEqual(
      expect.arrayContaining(["pi-parnaiba", "pi-floriano"]),
    );
  });
});

describe("buildScenario", () => {
  it("falha quando falta geometria de alguma província", () => {
    const data = {
      provinces: [{ id: "x" }],
      connections: [],
      states: [],
      terrains: [],
      nations: [],
    } as unknown as ScenarioData;
    const geometry: GeometryCollection = { type: "FeatureCollection", features: [] };

    expect(() => buildScenario(data, geometry, {})).toThrow(/x/);
  });
});

describe("loadScenarioFromApi", () => {
  it("busca o cenário pela API e usa os espaços de construção do servidor", async () => {
    const response = scenarioResponseFixture();
    response.buildingSlots["pe-recife"] = 5;
    const api = { get: vi.fn().mockResolvedValue(response), post: vi.fn() };

    const scenario = await loadScenarioFromApi(api);

    expect(api.get).toHaveBeenCalledWith("/api/v1/scenarios/nordeste-teste", expect.anything());
    expect(scenario.provinces.size).toBe(24);
    expect(scenario.buildingSlots.get("pe-recife")).toBe(5);
  });
});
