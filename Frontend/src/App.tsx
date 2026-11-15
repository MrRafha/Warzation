import { useCallback, useMemo, type ReactNode } from "react";
import { ConnectionStatus } from "./features/connection/ConnectionStatus";
import { useBackendHealth } from "./features/connection/useBackendHealth";
import { DevRoomPanel } from "./features/devRoom/DevRoomPanel";
import { MapScreen } from "./features/map/MapScreen";
import { loadScenarioFromApi } from "./features/map/scenario";
import { createApiClient } from "./lib/apiClient";
import { readConfig, type AppConfig } from "./lib/config";
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
  const api = useMemo(() => createApiClient(config.apiBaseUrl), [config.apiBaseUrl]);
  const realtime = useMemo(() => new RealtimeClient(config.wsUrl), [config.wsUrl]);
  const loadScenario = useCallback(() => loadScenarioFromApi(api), [api]);
  const { health, retry } = useBackendHealth(api);
  const online = health.status === "online";

  return (
    <Layout
      status={<ConnectionStatus health={health} onRetry={retry} />}
      main={
        online ? (
          <MapScreen loadScenario={loadScenario} />
        ) : (
          health.status === "offline" && (
            <p className="muted map-placeholder">O mapa aparece quando o Backend estiver conectado.</p>
          )
        )
      }
      devTools={online ? <DevRoomPanel client={realtime} /> : null}
    />
  );
}

function Layout({ status, main, devTools }: { status: ReactNode; main?: ReactNode; devTools?: ReactNode }) {
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

      {devTools && (
        <details className="dev-tools">
          <summary>Ferramentas de desenvolvimento</summary>
          {devTools}
        </details>
      )}
    </main>
  );
}
