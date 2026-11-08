import { useEffect, useState } from "react";
import "./styles/app.css";

type ConnectionStatus = "checking" | "offline" | "online";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export function App() {
  const [connectionStatus, setConnectionStatus] =
    useState<ConnectionStatus>("checking");

  useEffect(() => {
    const controller = new AbortController();

    fetch(`${apiBaseUrl}/actuator/health`, {
      signal: controller.signal,
    })
      .then((response) => {
        setConnectionStatus(response.ok ? "online" : "offline");
      })
      .catch(() => {
        setConnectionStatus("offline");
      });

    return () => controller.abort();
  }, []);

  const statusLabel = {
    checking: "Verificando conexão...",
    offline: "Backend offline",
    online: "Backend conectado",
  }[connectionStatus];

  return (
    <main className="app-shell">
      <section className="hero-card" aria-labelledby="project-title">
        <p className="eyebrow">Grand strategy multiplayer</p>
        <h1 id="project-title">Warzation</h1>
        <p className="hero-copy">
          Construa uma civilização, governe suas províncias e conquiste seu
          espaço em um mundo persistente.
        </p>
        <div className={`connection-status ${connectionStatus}`} role="status">
          <span aria-hidden="true" />
          {statusLabel}
        </div>
      </section>
    </main>
  );
}
