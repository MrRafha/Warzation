"""Valida um cenário de Backend/data/scenarios contra as regras do mapa.

Uso: python tools/mapdata/validate_scenario.py Backend/data/scenarios/<cenario>

Confere:
- referências internas (terrenos, províncias, nações) e recursos conhecidos;
- adjacências sem duplicatas, sem laços e entre províncias existentes;
- grafo conexo e com 8 a 12 províncias no protótipo;
- nações com capital entre as províncias iniciais, três províncias contíguas e
  sem sobreposição entre nações;
- ausência de construções ou estradas nos dados;
- geometrias válidas, uma por província, e adjacências iguais às fronteiras
  que de fato existem na geometria.
"""
import itertools
import json
import sys
from collections import deque
from pathlib import Path

from shapely.geometry import Point, shape

RESOURCES = {"PEDRA", "METAL", "PETROLEO"}
ABUNDANCES = {"BAIXA", "MEDIA", "ALTA"}
FORBIDDEN_KEYS = {"buildings", "construcoes", "roads", "estradas"}
BORDER_TOLERANCE_DEG = 0.002

errors: list[str] = []


def fail(message: str) -> None:
    errors.append(message)


def walk_keys(value, path="$"):
    if isinstance(value, dict):
        for key, child in value.items():
            if key in FORBIDDEN_KEYS:
                fail(f"{path}.{key}: o cenário não pode trazer construções ou estradas")
            walk_keys(child, f"{path}.{key}")
    elif isinstance(value, list):
        for index, child in enumerate(value):
            walk_keys(child, f"{path}[{index}]")


def connected(nodes: set[str], edges: set[frozenset]) -> bool:
    if not nodes:
        return False
    neighbours = {node: set() for node in nodes}
    for edge in edges:
        a, b = tuple(edge)
        neighbours[a].add(b)
        neighbours[b].add(a)
    start = next(iter(nodes))
    seen, queue = {start}, deque([start])
    while queue:
        for nxt in neighbours[queue.popleft()] - seen:
            seen.add(nxt)
            queue.append(nxt)
    return seen == nodes


def main(directory: Path) -> int:
    scenario = json.loads((directory / "scenario.json").read_text(encoding="utf-8"))
    walk_keys(scenario)

    terrains = {t["id"] for t in scenario["terrains"]}
    provinces = scenario["provinces"]
    ids = [p["id"] for p in provinces]
    id_set = set(ids)

    if len(ids) != len(id_set):
        fail("identificadores de província duplicados")
    if not 8 <= len(id_set) <= 12:
        fail(f"o protótipo precisa de 8 a 12 províncias; há {len(id_set)}")

    for p in provinces:
        where = f"província {p['id']}"
        if p["terrain"] not in terrains:
            fail(f"{where}: terreno desconhecido {p['terrain']}")
        if not isinstance(p.get("coastal"), bool):
            fail(f"{where}: 'coastal' precisa ser true ou false")
        city = p.get("mainCity", {})
        if not city.get("name") or not isinstance(city.get("lon"), (int, float)) or not isinstance(city.get("lat"), (int, float)):
            fail(f"{where}: cidade principal incompleta")
        seen_resources = set()
        for deposit in p.get("deposits", []):
            if deposit["resource"] not in RESOURCES:
                fail(f"{where}: recurso desconhecido {deposit['resource']}")
            if deposit["abundance"] not in ABUNDANCES:
                fail(f"{where}: abundância desconhecida {deposit['abundance']}")
            if deposit["resource"] in seen_resources:
                fail(f"{where}: depósito de {deposit['resource']} repetido")
            seen_resources.add(deposit["resource"])

    edges: set[frozenset] = set()
    for a, b in scenario["connections"]:
        if a == b:
            fail(f"conexão de {a} com ela mesma")
            continue
        for node in (a, b):
            if node not in id_set:
                fail(f"conexão cita província inexistente {node}")
        edge = frozenset((a, b))
        if edge in edges:
            fail(f"conexão duplicada {a} - {b}")
        edges.add(edge)

    if not connected(id_set, edges):
        fail("o grafo de províncias não é conexo")

    claimed: dict[str, str] = {}
    for nation in scenario["nations"]:
        where = f"nação {nation['id']}"
        start = nation["startingProvinces"]
        if len(start) != 3 or len(set(start)) != 3:
            fail(f"{where}: precisa de exatamente três províncias iniciais distintas")
        if nation["capital"] not in start:
            fail(f"{where}: a capital precisa estar entre as províncias iniciais")
        for province in start:
            if province not in id_set:
                fail(f"{where}: província inexistente {province}")
            elif province in claimed:
                fail(f"{where}: {province} já pertence a {claimed[province]}")
            claimed[province] = nation["id"]
        inner = {e for e in edges if e <= set(start)}
        if set(start) <= id_set and not connected(set(start), inner):
            fail(f"{where}: as províncias iniciais precisam ser contíguas")

    geometry = json.loads((directory / scenario["geometryFile"]).read_text(encoding="utf-8"))
    shapes = {}
    for feature in geometry["features"]:
        pid = feature["properties"]["id"]
        if pid in shapes:
            fail(f"geometria duplicada para {pid}")
        geom = shape(feature["geometry"])
        if not geom.is_valid:
            fail(f"geometria inválida para {pid}")
        shapes[pid] = geom
    if set(shapes) != id_set:
        fail(f"geometrias e províncias não batem: {sorted(set(shapes) ^ id_set)}")
    else:
        for p in provinces:
            city = p["mainCity"]
            if not shapes[p["id"]].buffer(0.1).contains(Point(city["lon"], city["lat"])):
                fail(f"cidade principal de {p['id']} fica fora da província")
        touching = {
            frozenset((a, b))
            for a, b in itertools.combinations(sorted(shapes), 2)
            if not shapes[a].buffer(BORDER_TOLERANCE_DEG).intersection(shapes[b].buffer(BORDER_TOLERANCE_DEG)).is_empty
        }
        for edge in sorted(touching - edges, key=sorted):
            fail(f"fronteira na geometria sem conexão cadastrada: {' - '.join(sorted(edge))}")
        for edge in sorted(edges - touching, key=sorted):
            fail(f"conexão cadastrada sem fronteira na geometria: {' - '.join(sorted(edge))}")

    if errors:
        print(f"{directory}: {len(errors)} problema(s)")
        for message in errors:
            print(f"  - {message}")
        return 1
    print(f"{directory}: ok ({len(id_set)} províncias, {len(edges)} conexões, {len(scenario['nations'])} nações)")
    return 0


if __name__ == "__main__":
    sys.exit(main(Path(sys.argv[1])))
