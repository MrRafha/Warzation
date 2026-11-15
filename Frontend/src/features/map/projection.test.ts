import { describe, expect, it } from "vitest";
import { createProjection, stateBorderPath, toPath } from "./projection";
import type { ProvinceGeometry } from "./types";

const square = (x: number, y: number): ProvinceGeometry => ({
  type: "Polygon",
  coordinates: [[[x, y], [x + 1, y], [x + 1, y + 1], [x, y + 1], [x, y]]],
});

describe("createProjection", () => {
  it("encaixa as geometrias na largura pedida, com norte para cima", () => {
    const projection = createProjection([square(0, 0), square(1, 0)], 200, 0);

    expect(projection.width).toBe(200);
    expect(projection.height).toBe(100);
    expect(projection.project(0, 1)).toEqual([0, 0]);
    const [x, y] = projection.project(2, 0);
    expect(x).toBeCloseTo(200);
    expect(y).toBeCloseTo(100);
  });

  it("recusa conjunto vazio", () => {
    expect(() => createProjection([])).toThrow();
  });
});

describe("toPath", () => {
  it("gera um subcaminho fechado por anel, inclusive em MultiPolygon", () => {
    const projection = createProjection([square(0, 0)], 100, 0);
    const multi: ProvinceGeometry = {
      type: "MultiPolygon",
      coordinates: [square(0, 0).coordinates as never, square(0, 0).coordinates as never],
    };

    expect(toPath(square(0, 0), projection).match(/M/g)).toHaveLength(1);
    expect(toPath(multi, projection).match(/Z/g)).toHaveLength(2);
  });
});

describe("stateBorderPath", () => {
  const geometries = new Map<string, ProvinceGeometry>([
    ["a", square(0, 0)],
    ["b", square(1, 0)],
  ]);
  const projection = createProjection(geometries.values(), 200, 0);
  const segments = (path: string) => path.match(/M/g)?.length ?? 0;

  it("não traça a fronteira entre províncias do mesmo estado", () => {
    // 8 lados no total, 1 compartilhado: sobram 6 de contorno externo
    expect(segments(stateBorderPath(geometries, () => "PI", projection))).toBe(6);
  });

  it("traça a fronteira entre estados diferentes", () => {
    expect(segments(stateBorderPath(geometries, (id) => (id === "a" ? "PI" : "CE"), projection))).toBe(7);
  });
});
