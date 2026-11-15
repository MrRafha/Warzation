import { describe, expect, it } from "vitest";
import { matchState, matchView } from "../../test/matchFixtures";
import { NEUTRAL_COLOR } from "../map/labels";
import { buildMatchOverlay } from "./matchOverlay";

describe("buildMatchOverlay", () => {
  it("colore por nação, identifica o jogador e lista unidades e estoque", () => {
    const overlay = buildMatchOverlay(matchView(), matchState());

    const teresina = overlay.details.get("pi-teresina")!;
    expect(teresina.owner?.label).toBe("Nação do Parnaíba (Jogador 1)");
    expect(teresina.units).toEqual([{ label: "Infantaria básica", quantity: 2, neutral: false }]);
    expect(teresina.stock).toEqual([
      { label: "Comida", amount: 60 },
      { label: "Pedra", amount: 20 },
    ]);
    expect(overlay.details.get("ba-salvador")!.owner?.color).not.toBe(teresina.owner?.color);

    const recife = overlay.details.get("pe-recife")!;
    expect(recife.owner).toBeNull();
    expect(recife.units[0].neutral).toBe(true);
    expect(overlay.legend.at(-1)).toEqual({ label: "Neutra", color: NEUTRAL_COLOR });
  });
});
