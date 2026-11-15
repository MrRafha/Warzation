import { useMemo } from "react";
import { ConnectionStatus } from "./features/connection/ConnectionStatus";
import { useBackendHealth } from "./features/connection/useBackendHealth";
import { DevRoomPanel } from "./features/devRoom/DevRoomPanel";
import { createApiClient } from "./lib/apiClient";
import { readConfig, type AppConfig } from "./lib/config";
import { RealtimeClient } from "./lib/realtimeClient";
import "./styles/app.css";

export function App() {
  const configResult = useMemo(() => readConfig(), []);

  return (
    <main className="app-shell">
      <section className="hero-card" aria-labelledby="project-title">
        <p className="eyebrow">Grand strategy multiplayer</p>
        <h1 id="project-title">Warzation</h1>
        <p className="hero-copy">
          Construa uma civilização, governe suas províncias e conquiste seu
          espaço em um mundo persistente.
        </p>
        {configResult.ok ? (
          <ConnectedContent config={configResult.config} />
        ) : (
          <p className="config-error" role="alert">
            Configuração ausente: defina {configResult.missing.join(" e ")} em{" "}
            <code>Frontend/.env</code> (veja <code>.env.example</code>).
          </p>
        )}
      </section>
    </main>
  );
}

function ConnectedContent({ config }: { config: AppConfig }) {
  const api = useMemo(() => createApiClient(config.apiBaseUrl), [config.apiBaseUrl]);
  const realtime = useMemo(() => new RealtimeClient(config.wsUrl), [config.wsUrl]);
  const { health, retry } = useBackendHealth(api);

  return (
    <>
      <ConnectionStatus health={health} onRetry={retry} />
      {health.status === "online" && <DevRoomPanel client={realtime} />}
    </>
  );
}
