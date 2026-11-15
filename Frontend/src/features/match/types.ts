/** Formato de MatchView do Backend (GET /api/v1/matches). */
export type MatchView = {
  id: string;
  name: string;
  scenarioId: string;
  status: "OPEN" | "STARTED" | "FINISHED";
  maxPlayers: number;
  createdBy: string;
  createdAt: string;
  startedAt: string | null;
  /** Relógio econômico; ausente ou nulo antes de a partida começar. */
  economy?: { cycles: number; skippedCycles: number; nextCycleAt: string } | null;
  participants: Array<{ playerId: string; playerName: string; nationId: string; joinedAt: string }>;
  nations: Array<{ id: string; name: string; capital: string; playerId: string | null }>;
};

export type MatchCommandResult = {
  commandId: string;
  status: "APPLIED" | "DUPLICATE";
  match: MatchView;
};

/** Estado territorial oficial (GET /api/v1/matches/{id}/state). */
export type MatchStateView = {
  matchId: string;
  status: MatchView["status"];
  startedAt: string | null;
  provinces: Array<{
    provinceId: string;
    ownerNationId: string | null;
    ownerPlayerId: string | null;
    units: Array<{ type: string; quantity: number; ownerNationId: string | null }>;
    stock: Record<string, number>;
  }>;
};

/** Payload de MATCH_STATE no WebSocket. */
export type MatchSnapshot = {
  match: MatchView;
  state: MatchStateView;
};

/** Evento do histórico (GET /api/v1/matches/{id}/events). */
export type MatchEvent = {
  seq: number;
  type: string;
  playerId: string | null;
  playerName: string | null;
  commandId: string | null;
  occurredAt: string;
  data: Record<string, unknown>;
};
