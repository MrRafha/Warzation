/** Formato de MatchView do Backend (GET /api/v1/matches). */
export type MatchView = {
  id: string;
  name: string;
  scenarioId: string;
  status: "OPEN" | "STARTED" | "FINISHED";
  maxPlayers: number;
  createdBy: string;
  createdAt: string;
  participants: Array<{ playerId: string; playerName: string; nationId: string; joinedAt: string }>;
  nations: Array<{ id: string; name: string; capital: string; playerId: string | null }>;
};

export type MatchCommandResult = {
  commandId: string;
  status: "APPLIED" | "DUPLICATE";
  match: MatchView;
};
