import { useState, type ReactNode } from "react";
import { DEFAULT_TERRAIN_COLOR, terrainColor } from "./labels";
import { ProvinceList } from "./ProvinceList";
import { ProvincePanel } from "./ProvincePanel";
import { ScenarioMap } from "./ScenarioMap";
import type { MapOverlay, Scenario } from "./types";
import { useScenario } from "./useScenario";

type Props = {
  loadScenario: () => Promise<Scenario>;
  /** Conteúdo extra no topo da coluna lateral (ex.: lobby de partidas). */
  aside?: ReactNode;
  /** Estado da partida desenhado sobre o mapa. */
  overlay?: MapOverlay;
};

export function MapScreen({ loadScenario, aside, overlay }: Props) {
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
        <ScenarioMap scenario={scenario} selectedId={selectedId} onSelect={setSelectedId} overlay={overlay} />
        {overlay ? (
          <ul className="terrain-legend" aria-label="Legenda de donos">
            {overlay.legend.map((item) => (
              <li key={item.label}>
                <span style={{ background: item.color }} aria-hidden="true" />
                {item.label}
              </li>
            ))}
          </ul>
        ) : (
          <ul className="terrain-legend" aria-label="Legenda de terrenos">
            {scenario.data.terrains.map((terrain) => (
              <li key={terrain.id}>
                <span style={{ background: terrainColor[terrain.id] ?? DEFAULT_TERRAIN_COLOR }} aria-hidden="true" />
                {terrain.name}
              </li>
            ))}
          </ul>
        )}
      </div>
      <div className="side-column">
        {aside}
        <ProvincePanel
          scenario={scenario}
          provinceId={selectedId}
          onSelect={setSelectedId}
          details={selectedId ? overlay?.details.get(selectedId) : undefined}
        />
        <ProvinceList scenario={scenario} selectedId={selectedId} onSelect={setSelectedId} />
      </div>
    </div>
  );
}
