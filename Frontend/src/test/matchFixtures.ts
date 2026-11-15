import type { MatchStateView, MatchView } from "../features/match/types";

export function matchView(overrides: Partial<MatchView> = {}): MatchView {
  return {
    id: "m1",
    name: "Partida de teste",
    scenarioId: "nordeste-teste",
    status: "STARTED",
    maxPlayers: 2,
    createdBy: "jogador-1",
    createdAt: "2026-11-15T12:00:00Z",
    startedAt: "2026-11-15T12:05:00Z",
    participants: [
      { playerId: "jogador-1", playerName: "Jogador 1", nationId: "parnaiba", joinedAt: "" },
      { playerId: "jogador-2", playerName: "Jogador 2", nationId: "sao-francisco", joinedAt: "" },
    ],
    nations: [
      { id: "parnaiba", name: "Nação do Parnaíba", capital: "pi-teresina", playerId: "jogador-1" },
      { id: "sao-francisco", name: "Nação do São Francisco", capital: "ba-salvador", playerId: "jogador-2" },
    ],
    ...overrides,
  };
}

/** Estado inicial resumido: Teresina do jogador 1, Salvador do jogador 2, Recife neutra. */
export function matchState(overrides: Partial<MatchStateView> = {}): MatchStateView {
  return {
    matchId: "m1",
    status: "STARTED",
    startedAt: "2026-11-15T12:05:00Z",
    provinces: [
      {
        provinceId: "pi-teresina",
        ownerNationId: "parnaiba",
        ownerPlayerId: "jogador-1",
        units: [{ type: "INFANTARIA_BASICA", quantity: 2, ownerNationId: "parnaiba" }],
        stock: { COMIDA: 60, PEDRA: 20 },
      },
      {
        provinceId: "ba-salvador",
        ownerNationId: "sao-francisco",
        ownerPlayerId: "jogador-2",
        units: [{ type: "INFANTARIA_BASICA", quantity: 2, ownerNationId: "sao-francisco" }],
        stock: { COMIDA: 60, PEDRA: 20 },
      },
      {
        provinceId: "pe-recife",
        ownerNationId: null,
        ownerPlayerId: null,
        units: [{ type: "INFANTARIA_BASICA", quantity: 1, ownerNationId: null }],
        stock: {},
      },
    ],
    ...overrides,
  };
}
