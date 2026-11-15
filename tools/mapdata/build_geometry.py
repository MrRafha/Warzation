"""Gera provinces.geojson do cenário nordeste-teste a partir da malha estadual.

Uso: python tools/mapdata/build_geometry.py <br_states.json> <saida.geojson> <tolerancia_graus>

Mantém o polígono principal de cada estado, simplifica com topologia
compartilhada (as fronteiras continuam idênticas dos dois lados) e imprime o
erro de área e as fronteiras encontradas. Detalhes em Backend/data/README.md.
"""
import json, sys, itertools, warnings
warnings.filterwarnings("ignore")
from shapely.geometry import shape, Point, mapping, Polygon
from shapely.validation import make_valid
from shapely.ops import transform
import pyproj, topojson as tp

src, out, epsilon = sys.argv[1], sys.argv[2], float(sys.argv[3])
IDS = {"MA": "maranhao", "PI": "piaui", "CE": "ceara", "RN": "rio-grande-do-norte", "PB": "paraiba",
       "PE": "pernambuco", "AL": "alagoas", "SE": "sergipe", "BA": "bahia"}
CAPITALS = {"MA": (-44.30, -2.53), "PI": (-42.80, -5.09), "CE": (-38.54, -3.73), "RN": (-35.21, -5.79),
            "PB": (-34.86, -7.12), "PE": (-34.88, -8.05), "AL": (-35.73, -9.65), "SE": (-37.07, -10.91),
            "BA": (-38.50, -12.97)}
proj = pyproj.Transformer.from_crs("EPSG:4326", "ESRI:102033", always_xy=True).transform

data = json.load(open(src))
features = []
for f in data["features"]:
    uf = f["properties"]["SIGLA"]
    if uf not in IDS:
        continue
    geom = make_valid(shape(f["geometry"]))
    polys = [p for p in getattr(geom, "geoms", [geom]) if isinstance(p, Polygon)]
    main = max(polys, key=lambda p: p.area)  # descarta ilhas pequenas (ex.: Fernando de Noronha)
    main = Polygon(main.exterior)            # sem buracos
    assert main.buffer(0.05).contains(Point(CAPITALS[uf])), f"capital fora do polígono principal: {uf}"
    features.append({"type": "Feature", "properties": {"id": IDS[uf], "uf": uf}, "geometry": mapping(main)})

fc = {"type": "FeatureCollection", "features": features}
topo = tp.Topology(fc, prequantize=False, toposimplify=epsilon, topology=True)
simplified = json.loads(topo.to_geojson())

def rnd(coords):
    return [[round(x, 4), round(y, 4)] for x, y in coords]

result = []
for f in simplified["features"]:
    g = shape(f["geometry"])
    if g.geom_type != "Polygon":
        g = max(getattr(g, "geoms", [g]), key=lambda p: p.area)
    ring = rnd(list(g.exterior.coords))
    result.append({"type": "Feature", "properties": {"id": f["properties"]["id"]},
                   "geometry": {"type": "Polygon", "coordinates": [ring]}})
result.sort(key=lambda f: list(IDS.values()).index(f["properties"]["id"]))
json.dump({"type": "FeatureCollection", "features": result}, open(out, "w"), ensure_ascii=False, separators=(",", ":"))

# relatório
orig = {IDS[f["properties"]["uf"]]: shape(f["geometry"]) for f in features}
simp = {f["properties"]["id"]: shape(f["geometry"]) for f in result}
total = 0
for k, g in simp.items():
    n = len(g.exterior.coords); total += n
    err = transform(proj, g).area / transform(proj, orig[k]).area - 1
    print(f"{k:22} pontos={n:4} valido={g.is_valid} erro_area={err:+.2%}")
edges = []
for a, b in itertools.combinations(simp, 2):
    shared = simp[a].intersection(simp[b])
    over = simp[a].buffer(-0.001).intersection(simp[b].buffer(-0.001)).area
    if not simp[a].buffer(0.002).intersection(simp[b].buffer(0.002)).is_empty:
        edges.append((a, b))
        print(f"  {a} - {b}: fronteira compartilhada={shared.length:.2f}° sobreposição={over:.6f}")
print("pontos totais", total, "arestas", len(edges))
