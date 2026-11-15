import type { PolygonCoords, ProvinceGeometry, Ring } from "./types";

export type Projection = {
  width: number;
  height: number;
  project: (lon: number, lat: number) => [number, number];
};

/**
 * Projeção equiretangular com correção pela latitude média. Para uma região
 * do tamanho do Nordeste, perto do equador, a distorção é desprezível.
 */
export function createProjection(geometries: Iterable<ProvinceGeometry>, width = 1000, padding = 12): Projection {
  let minLon = Infinity;
  let maxLon = -Infinity;
  let minLat = Infinity;
  let maxLat = -Infinity;
  for (const geometry of geometries) {
    for (const ring of rings(geometry)) {
      for (const [lon, lat] of ring) {
        minLon = Math.min(minLon, lon);
        maxLon = Math.max(maxLon, lon);
        minLat = Math.min(minLat, lat);
        maxLat = Math.max(maxLat, lat);
      }
    }
  }
  if (!Number.isFinite(minLon)) {
    throw new Error("Nenhuma coordenada para projetar");
  }

  const xScale = Math.cos((((minLat + maxLat) / 2) * Math.PI) / 180);
  const spanX = (maxLon - minLon) * xScale;
  const spanY = maxLat - minLat;
  const k = (width - padding * 2) / spanX;
  const height = Math.round(spanY * k + padding * 2);

  return {
    width,
    height,
    project: (lon, lat) => [padding + (lon - minLon) * xScale * k, padding + (maxLat - lat) * k],
  };
}

export function rings(geometry: ProvinceGeometry): Ring[] {
  const polygons: PolygonCoords[] = geometry.type === "Polygon" ? [geometry.coordinates] : geometry.coordinates;
  return polygons.flat();
}

export function toPath(geometry: ProvinceGeometry, projection: Projection): string {
  return rings(geometry)
    .map((ring) => {
      const points = ring.map(([lon, lat]) => projection.project(lon, lat));
      return "M" + points.map(([x, y]) => `${x.toFixed(1)},${y.toFixed(1)}`).join("L") + "Z";
    })
    .join("");
}

/**
 * Traça as divisas entre estados e o contorno externo. Como as geometrias têm
 * topologia compartilhada, uma fronteira interna aparece com as mesmas
 * coordenadas nas duas províncias; um segmento usado por províncias de estados
 * diferentes, ou por uma só província, é divisa estadual.
 */
export function stateBorderPath(
  geometries: Map<string, ProvinceGeometry>,
  stateOf: (provinceId: string) => string,
  projection: Projection,
): string {
  const segments = new Map<string, { a: [number, number]; b: [number, number]; states: Set<string>; uses: number }>();
  for (const [id, geometry] of geometries) {
    const state = stateOf(id);
    for (const ring of rings(geometry)) {
      for (let i = 0; i < ring.length - 1; i++) {
        const a = ring[i];
        const b = ring[i + 1];
        const key = segmentKey(a, b);
        const entry = segments.get(key) ?? { a, b, states: new Set<string>(), uses: 0 };
        entry.states.add(state);
        entry.uses += 1;
        segments.set(key, entry);
      }
    }
  }

  let path = "";
  for (const { a, b, states, uses } of segments.values()) {
    if (uses === 1 || states.size > 1) {
      const [x1, y1] = projection.project(a[0], a[1]);
      const [x2, y2] = projection.project(b[0], b[1]);
      path += `M${x1.toFixed(1)},${y1.toFixed(1)}L${x2.toFixed(1)},${y2.toFixed(1)}`;
    }
  }
  return path;
}

function segmentKey(a: [number, number], b: [number, number]): string {
  const p = `${a[0]},${a[1]}`;
  const q = `${b[0]},${b[1]}`;
  return p < q ? `${p}|${q}` : `${q}|${p}`;
}
