import { useCallback, useMemo, useState, type ReactNode } from "react";
import { ConnectionStatus } from "./features/connection/ConnectionStatus";
import { useBackendHealth } from "./features/connection/useBackendHealth";
import { DevRoomPanel } from "./features/devRoom/DevRoomPanel";
import { MapScreen } from "./features/map/MapScreen";
import { loadScenarioFromApi } from "./features/map/scenario";
import { PlayerBar } from "./features/player/PlayerBar";
import { usePlayerSession, type PlayerSession } from "./features/player/usePlayerSession";
import { MatchLobby } from "./features/match/MatchLobby";
import { MatchScreen } from "./features/match/MatchScreen";
import { createApiClient } from "./lib/apiClient";
import { readConfig, type AppConfig } from "./lib/config";
import { readStoredToken } from "./lib/playerSession";
import { RealtimeClient } from "./lib/realtimeClient";
import "./styles/app.css";

export function App() {
  const configResult = useMemo(() => readConfig(), []);

  if (!configResult.ok) {
    return (
      <Layout
        status={
          <p className="config-error" role="alert">
            Configuração ausente: defina {configResult.missing.join(" e ")} em <code>Frontend/.env</code> (veja{" "}
            <code>.env.example</code>).
          </p>
        }
      />
    );
  }
  return <ConnectedApp config={configResult.config} />;
}

function ConnectedApp({ config }: { config: AppConfig }) {
  const api = useMemo(() => createApiClient(config.apiBaseUrl, undefined, readStoredToken), [config.apiBaseUrl]);
  const loadScenario = useCallback(() => loadScenarioFromApi(api), [api]);
  const { health, retry } = useBackendHealth(api);
  const { session, login, logout } = usePlayerSession(api);
  const online = health.status === "online";
  const playerId = session.status === "authenticated" ? session.player.id : null;
  const [openMatchId, setOpenMatchId] = useState<string | null>(null);
  const token = session.status === "authenticated" ? session.token : null;
  const matchRealtime = useMemo(
    () => (openMatchId && token ? new RealtimeClient(config.wsUrl, undefined, token) : null),
    [config.wsUrl, openMatchId, token],
  );

  return (
    <Layout
      status={
        <div className="status-column">
          <ConnectionStatus health={health} onRetry={retry} />
          {online && (
            <PlayerArea session={session} onLogin={login} onLogout={logout} wsUrl={config.wsUrl} />
          )}
        </div>
      }
      main={
        online ? (
          openMatchId && playerId ? (
            <MatchScreen
              key={`${openMatchId}:${playerId}`}
              api={api}
              matchId={openMatchId}
              playerId={playerId}
              loadScenario={loadScenario}
              onBack={() => setOpenMatchId(null)}
              realtime={matchRealtime}
            />
          ) : (
            <MapScreen
              loadScenario={loadScenario}
              aside={
                <MatchLobby key={playerId ?? "anonimo"} api={api} playerId={playerId} onOpen={setOpenMatchId} />
              }
            />
          )
        ) : (
          health.status === "offline" && (
            <p className="muted map-placeholder">O mapa aparece quando o Backend estiver conectado.</p>
          )
        )
      }
    />
  );
}

type PlayerAreaProps = {
  session: PlayerSession;
  onLogin: (token: string) => void;
  onLogout: () => void;
  wsUrl: string;
};

/** Jogador da aba e ferramentas que dependem dele. */
function PlayerArea({ session, onLogin, onLogout, wsUrl }: PlayerAreaProps) {
  const token = session.status === "authenticated" ? session.token : null;
  const realtime = useMemo(() => (token ? new RealtimeClient(wsUrl, undefined, token) : null), [wsUrl, token]);

  return (
    <>
      <PlayerBar session={session} onLogin={onLogin} onLogout={onLogout} />
      <details className="dev-tools">
        <summary>Ferramentas de desenvolvimento</summary>
        {realtime ? (
          <DevRoomPanel key={token} client={realtime} />
        ) : (
          <p className="muted">Entre como jogador de teste para usar a sala de teste.</p>
        )}
      </details>
    </>
  );
}

function Layout({ status, main }: { status: ReactNode; main?: ReactNode }) {
  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Grand strategy multiplayer</p>
          <h1>Warzation</h1>
        </div>
        {status}
      </header>

      {main}
    </main>
  );
}
