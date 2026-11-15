import type { RealtimeClient } from "../../lib/realtimeClient";
import { useDevRoom } from "./useDevRoom";

const connectionLabel = {
  connecting: "Conectando ao tempo real...",
  open: "Tempo real conectado",
  reconnecting: "Reconectando...",
  closed: "Tempo real desconectado",
};

type Props = {
  client: RealtimeClient;
};

/**
 * Painel da sala de teste. Abra a página em duas abas: o "+1" de uma aba
 * aparece na outra, porque ambas recebem o mesmo ROOM_STATE do servidor.
 */
export function DevRoomPanel({ client }: Props) {
  const { connection, room, lastResult, error, increment, resendLast, canResend } = useDevRoom(client);
  const online = connection === "open";

  return (
    <section className="dev-room" aria-labelledby="dev-room-title">
      <div className="dev-room-header">
        <h2 id="dev-room-title">Sala de teste</h2>
        <span className={`realtime-status ${connection}`}>{connectionLabel[connection]}</span>
      </div>

      <p className="counter" aria-live="polite">
        <span className="muted">Contador oficial</span>
        <strong data-testid="room-counter">{room ? room.counter : "—"}</strong>
        {room && <small className="muted">versão {room.version}</small>}
      </p>

      <div className="dev-room-actions">
        <button type="button" onClick={increment} disabled={!online || !room}>
          +1
        </button>
        <button type="button" className="secondary" onClick={resendLast} disabled={!online || !canResend}>
          Reenviar último comando
        </button>
      </div>

      {lastResult && (
        <p className="muted" role="status">
          Último comando: {lastResult.status === "APPLIED" ? "aplicado" : "duplicado, não reaplicado"} (
          {lastResult.commandId.slice(0, 8)})
        </p>
      )}
      {error && (
        <p className="dev-room-error" role="alert">
          {error.message}
        </p>
      )}
    </section>
  );
}
