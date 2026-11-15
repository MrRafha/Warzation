import type { ApiClient } from "../../lib/apiClient";
import { newId } from "../../lib/realtimeClient";
import type { MatchCommandResult, MatchEvent, MatchStateView, MatchView } from "./types";

export function listMatches(api: ApiClient): Promise<MatchView[]> {
  return api.get<MatchView[]>("/api/v1/matches");
}

export function getMatch(api: ApiClient, matchId: string): Promise<MatchView> {
  return api.get<MatchView>(`/api/v1/matches/${encodeURIComponent(matchId)}`);
}

export function getMatchState(api: ApiClient, matchId: string): Promise<MatchStateView> {
  return api.get<MatchStateView>(`/api/v1/matches/${encodeURIComponent(matchId)}/state`);
}

export function startMatch(api: ApiClient, matchId: string): Promise<MatchCommandResult> {
  return api.post<MatchCommandResult>(`/api/v1/matches/${encodeURIComponent(matchId)}/start`, { commandId: newId() });
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

export function getMatchEvents(api: ApiClient, matchId: string): Promise<MatchEvent[]> {
  return api.get<MatchEvent[]>(`/api/v1/matches/${encodeURIComponent(matchId)}/events`);
}
