import type { BackendHealth } from "./useBackendHealth";

type Props = {
  health: BackendHealth;
  onRetry: () => void;
};

export function ConnectionStatus({ health, onRetry }: Props) {
  const label = {
    checking: "Verificando conexão...",
    online: "Backend conectado",
    offline: "Backend offline",
  }[health.status];

  return (
    <div className="connection-block">
      <div className={`connection-status ${health.status}`} role="status">
        <span aria-hidden="true" />
        {label}
        {health.status === "online" && (
          <small className="muted">v{health.info.version}</small>
        )}
      </div>
      {health.status === "offline" && (
        <p className="connection-detail">
          {health.reason}.{" "}
          <button type="button" className="link-button" onClick={onRetry}>
            Tentar novamente
          </button>
        </p>
      )}
    </div>
  );
}
