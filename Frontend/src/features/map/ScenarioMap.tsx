import { useMemo, useState, type KeyboardEvent } from "react";
import { DEFAULT_TERRAIN_COLOR, terrainColor } from "./labels";
import { createProjection, stateBorderPath, toPath } from "./projection";
import type { Scenario } from "./types";

type Props = {
  scenario: Scenario;
  selectedId: string | null;
  onSelect: (provinceId: string) => void;
};

/** Mapa em SVG. Só desenha e reporta seleção; nenhuma regra de jogo fica aqui. */
export function ScenarioMap({ scenario, selectedId, onSelect }: Props) {
  const [hoveredId, setHoveredId] = useState<string | null>(null);

  const drawing = useMemo(() => {
    const projection = createProjection(scenario.geometries.values());
    const provinces = scenario.data.provinces.map((province) => {
      const geometry = scenario.geometries.get(province.id)!;
      const [cx, cy] = projection.project(province.mainCity.lon, province.mainCity.lat);
      return { province, path: toPath(geometry, projection), cx, cy };
    });
    const borders = stateBorderPath(
      scenario.geometries,
      (id) => scenario.provinces.get(id)?.state ?? id,
      projection,
    );
    return { projection, provinces, borders };
  }, [scenario]);

  const neighbours = new Set(selectedId ? scenario.neighbours.get(selectedId) : []);
  const labelled = new Set([selectedId, hoveredId].filter((id): id is string => id !== null));

  function handleKey(event: KeyboardEvent, provinceId: string) {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onSelect(provinceId);
    }
  }

  return (
    <svg
      className="scenario-map"
      viewBox={`0 0 ${drawing.projection.width} ${drawing.projection.height}`}
      role="group"
      aria-label={`Mapa: ${scenario.data.name}`}
    >
      <g>
        {drawing.provinces.map(({ province, path }) => {
          const classes = ["province"];
          if (province.id === selectedId) classes.push("selected");
          if (neighbours.has(province.id)) classes.push("neighbour");
          if (province.id === hoveredId) classes.push("hovered");
          return (
            <path
              key={province.id}
              d={path}
              className={classes.join(" ")}
              fill={terrainColor[province.terrain] ?? DEFAULT_TERRAIN_COLOR}
              role="button"
              tabIndex={0}
              aria-label={`${province.name}, ${scenario.stateNames.get(province.state) ?? province.state}`}
              aria-pressed={province.id === selectedId}
              data-province-id={province.id}
              onClick={() => onSelect(province.id)}
              onKeyDown={(event) => handleKey(event, province.id)}
              onMouseEnter={() => setHoveredId(province.id)}
              onMouseLeave={() => setHoveredId((current) => (current === province.id ? null : current))}
              onFocus={() => setHoveredId(province.id)}
              onBlur={() => setHoveredId((current) => (current === province.id ? null : current))}
            >
              <title>{province.name}</title>
            </path>
          );
        })}
      </g>
      <path className="state-borders" d={drawing.borders} aria-hidden="true" />
      <g aria-hidden="true">
        {drawing.provinces.map(({ province, cx, cy }) => (
          <g key={province.id} className="city">
            <circle cx={cx} cy={cy} r={scenario.capitals.has(province.id) ? 5 : 3.5} />
            {labelled.has(province.id) && (
              <text x={cx} y={cy - 9} textAnchor="middle">
                {province.name}
              </text>
            )}
          </g>
        ))}
      </g>
    </svg>
  );
}
