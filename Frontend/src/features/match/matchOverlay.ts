import { NATION_COLORS, NEUTRAL_COLOR, stockLabel, unitLabel } from "../map/labels";
import type { MapOverlay, ProvinceDetails } from "../map/types";
import type { MatchStateView, MatchView } from "./types";

/** Converte o estado oficial da partida no que o mapa desenha. */
export function buildMatchOverlay(match: MatchView, state: MatchStateView): MapOverlay {
  const colorByNation = new Map(match.nations.map((nation, index) => [nation.id, NATION_COLORS[index % NATION_COLORS.length]]));
  const nationName = new Map(match.nations.map((nation) => [nation.id, nation.name]));
  const playerName = new Map(match.participants.map((p) => [p.playerId, p.playerName]));

  const details = new Map<string, ProvinceDetails>();
  for (const province of state.provinces) {
    const owner = province.ownerNationId
      ? {
          label: [
            nationName.get(province.ownerNationId) ?? province.ownerNationId,
            province.ownerPlayerId ? `(${playerName.get(province.ownerPlayerId) ?? province.ownerPlayerId})` : null,
          ]
            .filter(Boolean)
            .join(" "),
          color: colorByNation.get(province.ownerNationId) ?? NEUTRAL_COLOR,
        }
      : null;
    details.set(province.provinceId, {
      owner,
      units: province.units.map((unit) => ({
        label: unitLabel[unit.type] ?? unit.type,
        quantity: unit.quantity,
        neutral: unit.ownerNationId === null,
      })),
      stock: Object.entries(province.stock).map(([resource, amount]) => ({
        label: stockLabel[resource] ?? resource,
        amount,
      })),
    });
  }

  return {
    details,
    legend: [
      ...match.nations
        .filter((nation) => match.participants.some((p) => p.nationId === nation.id))
        .map((nation) => ({ label: nation.name, color: colorByNation.get(nation.id) ?? NEUTRAL_COLOR })),
      { label: "Neutra", color: NEUTRAL_COLOR },
    ],
  };
}
