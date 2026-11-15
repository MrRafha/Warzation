import { useCallback, useMemo, type ReactNode } from "react";
import { ConnectionStatus } from "./features/connection/ConnectionStatus";
import { useBackendHealth } from "./features/connection/useBackendHealth";
import { DevRoomPanel } from "./features/devRoom/DevRoomPanel";
import { MapScreen } from "./features/map/MapScreen";
import { loadScenarioFromApi } from "./features/map/scenario";
import { PlayerBar } from "./features/player/PlayerBar";
import { usePlayerSession } from "./features/player/usePlayerSession";
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
  const online = health.status === "online";

  return (
    <Layout
      status={
        <div className="status-column">
          <ConnectionStatus health={health} onRetry={retry} />
          {online && <PlayerArea api={api} wsUrl={config.wsUrl} />}
        </div>
      }
      main={
        online ? (
          <MapScreen loadScenario={loadScenario} />
        ) : (
          health.status === "offline" && (
            <p className="muted map-placeholder">O mapa aparece quando o Backend estiver conectado.</p>
          )
        )
      }
    />
  );
}

/** Jogador da aba e ferramentas que dependem dele. */
function PlayerArea({ api, wsUrl }: { api: ReturnType<typeof createApiClient>; wsUrl: string }) {
  const { session, login, logout } = usePlayerSession(api);
  const token = session.status === "authenticated" ? session.token : null;
  const realtime = useMemo(() => (token ? new RealtimeClient(wsUrl, undefined, token) : null), [wsUrl, token]);

  return (
    <>
      <PlayerBar session={session} onLogin={login} onLogout={logout} />
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
