"""Gera as províncias do cenário nordeste-teste a partir da malha municipal do IBGE.

Uso:
  python tools/mapdata/build_geometry.py <dir_malha_municipal> <br_states.json> <regions.json> <saida_dir>

- <dir_malha_municipal>: pasta `geojson/` de tbrugz/geodata-br (arquivos geojs-XX-mun.json)
- <br_states.json>: limites estaduais de giuliano-oliveira/geodata-br-states, usados só
  para separar o litoral das divisas com estados de fora do cenário
- <regions.json>: cidades-polo de cada estado (tools/mapdata/nordeste-regions.json)
- <saida_dir>: pasta do cenário; grava provinces.geojson, municipalities.json e
  derived.json (costa e conexões calculadas)

Cada município vai para a cidade-polo mais próxima do mesmo estado (distância
dividida pelo peso da cidade). Pedaços desconectados são devolvidos ao vizinho
com quem fazem mais fronteira, então toda província é contígua e todo limite
interno segue divisas municipais reais. A simplificação usa topologia
compartilhada, de modo que a fronteira entre duas províncias é a mesma linha
dos dois lados.
"""
import itertools
import json
import math
import sys
import warnings
from collections import defaultdict
from pathlib import Path

import pyproj
import topojson as tp
from shapely.geometry import MultiPolygon, Point, Polygon, mapping, shape
from shapely.ops import transform, unary_union
from shapely.strtree import STRtree
from shapely.validation import make_valid

warnings.filterwarnings("ignore")

UF_CODES = {"MA": 21, "PI": 22, "CE": 23, "RN": 24, "PB": 25, "PE": 26, "AL": 27, "SE": 28, "BA": 29}
MIN_PART_KM2 = 300          # descarta ilhas menores (ex.: Fernando de Noronha, Itaparica)
SIMPLIFY_DEG = 0.02
DECIMALS = 4
TOUCH_DEG = 0.002
COAST_MIN_DEG = 0.05        # ~5 km de litoral para contar como costeira

AREA = pyproj.Transformer.from_crs("EPSG:4326", "ESRI:102033", always_xy=True).transform


def km2(geom) -> float:
    return transform(AREA, geom).area / 1e6


def polygons(geom):
    return [g for g in getattr(geom, "geoms", [geom]) if isinstance(g, Polygon)]


def assign_state(municipalities, seeds):
    def nearest(geom):
        c = geom.representative_point()
        return min(seeds, key=lambda s: math.hypot(c.x - s["lon"], c.y - s["lat"]) / s.get("weight", 1))["id"]

    by_id = {m["id"]: m for m in municipalities}
    assign = {m["id"]: nearest(m["geom"]) for m in municipalities}
    seed_home = {}
    for s in seeds:
        home = [m["id"] for m in municipalities if m["geom"].buffer(0.001).contains(Point(s["lon"], s["lat"]))]
        if not home:
            raise SystemExit(f"cidade-polo {s['id']} fora da malha do estado")
        assign[home[0]] = s["id"]
        seed_home[s["id"]] = home[0]

    geoms = [m["geom"] for m in municipalities]
    tree = STRtree(geoms)
    neighbours = defaultdict(set)
    for i, m in enumerate(municipalities):
        grown = m["geom"].buffer(0.001)
        for j in tree.query(grown):
            if j != i and grown.intersects(geoms[j]):
                neighbours[m["id"]].add(municipalities[j]["id"])

    for _ in range(20):
        changed = False
        for s in seeds:
            members = {mid for mid, r in assign.items() if r == s["id"]}
            seen, stack = {seed_home[s["id"]]}, [seed_home[s["id"]]]
            while stack:
                for nxt in neighbours[stack.pop()]:
                    if nxt in members and nxt not in seen:
                        seen.add(nxt)
                        stack.append(nxt)
            for mid in members - seen:
                options = defaultdict(int)
                for nxt in neighbours[mid]:
                    if assign[nxt] != s["id"]:
                        options[assign[nxt]] += 1
                if options:
                    assign[mid] = max(options, key=options.get)
                    changed = True
        if not changed:
            break
    return assign, by_id


def main():
    mun_dir, states_file, regions_file, out_dir = map(Path, sys.argv[1:5])
    regions = json.loads(regions_file.read_text(encoding="utf-8"))

    provinces, members = {}, {}
    for uf, code in UF_CODES.items():
        raw = json.loads((mun_dir / f"geojs-{code}-mun.json").read_text(encoding="utf-8"))["features"]
        municipalities = [{"id": f["properties"]["id"], "geom": make_valid(shape(f["geometry"]))} for f in raw]
        assign, by_id = assign_state(municipalities, regions[uf])
        for seed in regions[uf]:
            ids = sorted(mid for mid, r in assign.items() if r == seed["id"])
            merged = unary_union([by_id[mid]["geom"] for mid in ids])
            parts = [Polygon(p.exterior) for p in polygons(merged) if km2(p) >= MIN_PART_KM2]
            provinces[seed["id"]] = parts[0] if len(parts) == 1 else MultiPolygon(parts)
            members[seed["id"]] = ids

    features = [{"type": "Feature", "properties": {"id": pid}, "geometry": mapping(g)} for pid, g in provinces.items()]
    topo = tp.Topology({"type": "FeatureCollection", "features": features},
                       prequantize=False, toposimplify=SIMPLIFY_DEG, topology=True)
    simplified = json.loads(topo.to_geojson())

    def rounded(poly):
        return [[[round(x, DECIMALS), round(y, DECIMALS)] for x, y in poly.exterior.coords]]

    result, shapes = [], {}
    order = list(provinces)
    for f in sorted(simplified["features"], key=lambda f: order.index(f["properties"]["id"])):
        parts = polygons(shape(f["geometry"]))
        if len(parts) == 1:
            geometry = {"type": "Polygon", "coordinates": rounded(parts[0])}
        else:
            geometry = {"type": "MultiPolygon", "coordinates": [rounded(p) for p in parts]}
        result.append({"type": "Feature", "properties": {"id": f["properties"]["id"]}, "geometry": geometry})
        shapes[f["properties"]["id"]] = shape(geometry)

    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "provinces.geojson").write_text(
        json.dumps({"type": "FeatureCollection", "features": result}, separators=(",", ":")), encoding="utf-8")
    (out_dir / "municipalities.json").write_text(
        json.dumps({"source": "IBGE (tbrugz/geodata-br)", "provinces": members}, ensure_ascii=False, indent=1),
        encoding="utf-8")

    # litoral = contorno externo do cenário menos as divisas com estados de fora
    others = [make_valid(shape(f["geometry"])) for f in json.loads(states_file.read_text())["features"]
              if f["properties"]["SIGLA"] not in UF_CODES]
    outside = unary_union(others).buffer(0.05)
    coastline = unary_union(list(shapes.values())).boundary.difference(outside)
    coastal = {pid: g.boundary.buffer(0.01).intersection(coastline).length >= COAST_MIN_DEG
               for pid, g in shapes.items()}
    connections = [[a, b] for a, b in itertools.combinations(order, 2)
                   if not shapes[a].buffer(TOUCH_DEG).intersection(shapes[b].buffer(TOUCH_DEG)).is_empty]
    (out_dir / "derived.json").write_text(
        json.dumps({"coastal": coastal, "connections": connections}, ensure_ascii=False, indent=1), encoding="utf-8")

    original = {pid: g for pid, g in provinces.items()}
    for pid in order:
        g = shapes[pid]
        err = km2(g) / km2(original[pid]) - 1
        print(f"{pid:26} {km2(g):>9,.0f} km²  erro={err:+.2%}  valido={g.is_valid}  costa={coastal[pid]}")
    print(f"{len(order)} províncias, {len(connections)} conexões")


if __name__ == "__main__":
    main()
