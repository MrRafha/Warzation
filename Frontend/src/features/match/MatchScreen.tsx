import { useCallback, useEffect, useMemo, useState } from "react";
import { ApiError, type ApiClient } from "../../lib/apiClient";
import type { ConnectionState, RealtimeClient } from "../../lib/realtimeClient";
import { MapScreen } from "../map/MapScreen";
import type { Scenario } from "../map/types";
import { getMatch, getMatchEvents, getMatchState, startMatch } from "./matchApi";
import { MatchHistory } from "./MatchHistory";
import { buildMatchOverlay } from "./matchOverlay";
import type { MatchEvent, MatchSnapshot, MatchStateView, MatchView } from "./types";

type Props = {
  api: ApiClient;
  matchId: string;
  playerId: string;
  loadScenario: () => Promise<Scenario>;
  onBack: () => void;
  /** Canal em tempo real; sem ele, a tela só atualiza ao agir. */
  realtime?: RealtimeClient | null;
};

const REALTIME_LABEL: Record<ConnectionState, string> = {
  connecting: "conectando ao tempo real...",
  open: "ao vivo",
  reconnecting: "reconectando...",
  closed: "tempo real desconectado",
};

const STATUS_LABEL: Record<MatchView["status"], string> = {
  OPEN: "aguardando início",
  STARTED: "em andamento",
  FINISHED: "encerrada",
};

/** Partida aberta no mapa: donos, unidades e estoques vindos do servidor. */
export function MatchScreen({ api, matchId, playerId, loadScenario, onBack, realtime }: Props) {
  const [match, setMatch] = useState<MatchView | null>(null);
  const [state, setState] = useState<MatchStateView | null>(null);
  const [events, setEvents] = useState<MatchEvent[]>([]);

  const reloadEvents = useCallback(async () => {
    try {
      setEvents(await getMatchEvents(api, matchId));
    } catch {
      // o histórico é complementar: sem ele, o resto da tela continua funcionando
    }
  }, [api, matchId]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const reload = useCallback(async () => {
    try {
      const [view, current] = await Promise.all([getMatch(api, matchId), getMatchState(api, matchId)]);
      setMatch(view);
      setState(current);
      setError(null);
      void reloadEvents();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Erro ao carregar a partida");
    }
  }, [api, matchId, reloadEvents]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const [live, setLive] = useState<ConnectionState | null>(null);

  // O servidor envia o estado completo ao entrar no canal e a cada mudança confirmada.
  useEffect(() => {
    if (!realtime) {
      return;
    }
    const stopState = realtime.onStateChange(setLive);
    const stopMessages = realtime.onMessage((message) => {
      if (message.roomId !== matchId) {
        return;
      }
      if (message.type === "MATCH_STATE") {
        const snapshot = message.payload as MatchSnapshot;
        setMatch(snapshot.match);
        setState(snapshot.state);
        void reloadEvents();
      } else if (message.type === "ERROR") {
        setError((message.payload as { message?: string }).message ?? "Erro no tempo real");
      }
    });
    realtime.joinMatch(matchId);
    realtime.connect();
    return () => {
      stopState();
      stopMessages();
      realtime.disconnect();
    };
  }, [realtime, matchId, reloadEvents]);

  const overlay = useMemo(
    () => (match && state && state.provinces.length > 0 ? buildMatchOverlay(match, state) : undefined),
    [match, state],
  );

  async function start() {
    setBusy(true);
    try {
      await startMatch(api, matchId);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : "Erro ao iniciar a partida");
    } finally {
      setBusy(false);
      await reload();
    }
  }

  const me = match?.participants.find((p) => p.playerId === playerId);
  const myNation = match?.nations.find((n) => n.id === me?.nationId);
  const canStart = match?.status === "OPEN" && match.createdBy === playerId;

  const header = (
    <section className="match-header" aria-labelledby="match-title">
      <button type="button" className="link-button" onClick={onBack}>
        ← Voltar às partidas
      </button>
      <h2 id="match-title">{match?.name ?? "Partida"}</h2>
      {match && (
        <p className="muted">
          {STATUS_LABEL[match.status]}
          {myNation && ` · você joga com ${myNation.name}`}
        </p>
      )}
      {live && <p className={`realtime-status ${live}`}>{REALTIME_LABEL[live]}</p>}
      {match && (
        <ul className="nation-slots">
          {match.nations.map((nation) => {
            const holder = match.participants.find((p) => p.nationId === nation.id);
            return (
              <li key={nation.id}>
                <span>{nation.name}</span>
                <span className="muted">{holder ? holder.playerName : "sem jogador"}</span>
              </li>
            );
          })}
        </ul>
      )}
      {canStart && (
        <button type="button" className="primary-button" onClick={() => void start()} disabled={busy}>
          Iniciar partida
        </button>
      )}
      {match?.status === "OPEN" && !canStart && <p className="muted">Aguardando quem criou iniciar a partida.</p>}
      {error && (
        <p className="lobby-error" role="alert">
          {error}
        </p>
      )}
      <MatchHistory events={events} match={match} />
    </section>
  );

  return <MapScreen loadScenario={loadScenario} aside={header} overlay={overlay} />;
}
