"""Gera uma imagem de conferência do cenário (donos iniciais, capitais e conexões).

Uso: python tools/mapdata/render_preview.py Backend/data/scenarios/<cenario>
Requer matplotlib (não faz parte de requirements.txt, que só cobre a validação).
"""
import json
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Patch
from shapely.geometry import shape
from shapely.ops import unary_union

PALETTE = ["#3f8f73", "#c78a3b", "#5b7fc7", "#b0566a", "#8a6fc2", "#6b9e3a"]
NEUTRAL = "#4a5560"

directory = Path(sys.argv[1])
scenario = json.loads((directory / "scenario.json").read_text(encoding="utf-8"))
geometry = json.loads((directory / scenario["geometryFile"]).read_text(encoding="utf-8"))

colors = {n["id"]: PALETTE[i % len(PALETTE)] for i, n in enumerate(scenario["nations"])}
owner = {p: n["id"] for n in scenario["nations"] for p in n["startingProvinces"]}
capitals = {n["capital"] for n in scenario["nations"]}
provinces = {p["id"]: p for p in scenario["provinces"]}
shapes = {f["properties"]["id"]: shape(f["geometry"]) for f in geometry["features"]}

fig, ax = plt.subplots(figsize=(10, 11), dpi=110)
fig.patch.set_facecolor("#101820")
ax.set_facecolor("#101820")
def parts(geom):
    return list(getattr(geom, "geoms", [geom]))


for pid, geom in shapes.items():
    for part in parts(geom):
        x, y = part.exterior.xy
        ax.fill(x, y, color=colors.get(owner.get(pid), NEUTRAL), alpha=0.9, ec="#eef4f1", lw=0.4)
for state in {p["state"] for p in scenario["provinces"]}:
    merged = unary_union([shapes[p["id"]].buffer(0.003) for p in scenario["provinces"] if p["state"] == state])
    for part in parts(merged):
        x, y = part.exterior.xy
        ax.plot(x, y, color="#eef4f1", lw=1.6)
for a, b in scenario["connections"]:
    pa, pb = shapes[a].representative_point(), shapes[b].representative_point()
    ax.plot([pa.x, pb.x], [pa.y, pb.y], color="#eef4f1", lw=0.5, ls=":", alpha=0.45)
for pid in shapes:
    city = provinces[pid]["mainCity"]
    label = provinces[pid]["name"] + (" ★" if pid in capitals else "")
    ax.plot(city["lon"], city["lat"], "o", color="white", ms=3)
    ax.text(city["lon"], city["lat"] + 0.1, label, color="white", ha="center", va="bottom", fontsize=7,
            weight="bold", bbox={"facecolor": "#101820", "alpha": 0.55, "pad": 1, "lw": 0})

legend = [Patch(color=colors[n["id"]], label=n["name"]) for n in scenario["nations"]]
legend.append(Patch(color=NEUTRAL, label="Neutra"))
ax.legend(handles=legend, loc="lower left", facecolor="#172b31", labelcolor="white", fontsize=8)
ax.set_aspect("equal")
ax.axis("off")
ax.set_title(f"Warzation — {scenario['name']}", color="white")
output = directory / "preview.png"
plt.savefig(output, bbox_inches="tight", facecolor=fig.get_facecolor())
print(f"gerado {output}")
