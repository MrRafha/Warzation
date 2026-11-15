import type { ApiClient } from "../../lib/apiClient";
import { newId } from "../../lib/realtimeClient";
import type { MatchCommandResult, MatchView } from "./types";

export function listOpenMatches(api: ApiClient): Promise<MatchView[]> {
  return api.get<MatchView[]>("/api/v1/matches?status=OPEN");
}

/** Cada ação gera um commandId novo; reenviar o mesmo id não repete o efeito no servidor. */
export function createMatch(api: ApiClient, scenarioId: string, name?: string): Promise<MatchCommandResult> {
  return api.post<MatchCommandResult>("/api/v1/matches", {
    commandId: newId(),
    scenarioId,
    ...(name?.trim() ? { name: name.trim() } : {}),
  });
}

export function joinMatch(api: ApiClient, matchId: string, nationId: string): Promise<MatchCommandResult> {
  return api.post<MatchCommandResult>(`/api/v1/matches/${encodeURIComponent(matchId)}/participants`, {
    commandId: newId(),
    nationId,
  });
}
