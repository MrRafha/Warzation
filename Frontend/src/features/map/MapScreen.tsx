import { useState } from "react";
import { DEFAULT_TERRAIN_COLOR, terrainColor } from "./labels";
import { ProvinceList } from "./ProvinceList";
import { ProvincePanel } from "./ProvincePanel";
import { ScenarioMap } from "./ScenarioMap";
import type { Scenario } from "./types";
import { useScenario } from "./useScenario";

type Props = {
  loadScenario: () => Promise<Scenario>;
};

export function MapScreen({ loadScenario }: Props) {
  const state = useScenario(loadScenario);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  if (state.status === "loading") {
    return <p className="muted" role="status">Carregando mapa...</p>;
  }
  if (state.status === "error") {
    return (
      <p className="config-error" role="alert">
        Não foi possível carregar o mapa: {state.message}
      </p>
    );
  }

  const { scenario } = state;
  return (
    <div className="map-screen">
      <div className="map-column">
        <ScenarioMap scenario={scenario} selectedId={selectedId} onSelect={setSelectedId} />
        <ul className="terrain-legend" aria-label="Legenda de terrenos">
          {scenario.data.terrains.map((terrain) => (
            <li key={terrain.id}>
              <span style={{ background: terrainColor[terrain.id] ?? DEFAULT_TERRAIN_COLOR }} aria-hidden="true" />
              {terrain.name}
            </li>
          ))}
        </ul>
      </div>
      <div className="side-column">
        <ProvincePanel scenario={scenario} provinceId={selectedId} onSelect={setSelectedId} />
        <ProvinceList scenario={scenario} selectedId={selectedId} onSelect={setSelectedId} />
      </div>
    </div>
  );
}
