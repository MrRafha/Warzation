import { useCallback, useEffect, useRef, useState } from "react";
import { newId, RealtimeClient, type ConnectionState } from "../../lib/realtimeClient";
import type { CommandResult, ErrorPayload, RoomCommand, RoomState } from "./types";

export const DEV_ROOM_ID = "dev";

/** Liga a interface à sala de teste via WebSocket. O estado exibido é sempre o do servidor. */
export function useDevRoom(client: RealtimeClient, roomId = DEV_ROOM_ID) {
  const [connection, setConnection] = useState<ConnectionState>(client.getState());
  const [room, setRoom] = useState<RoomState | null>(null);
  const [lastResult, setLastResult] = useState<CommandResult | null>(null);
  const [error, setError] = useState<ErrorPayload | null>(null);
  const lastCommand = useRef<RoomCommand | null>(null);

  useEffect(() => {
    const stopState = client.onStateChange(setConnection);
    const stopMessages = client.onMessage((message) => {
      if (message.roomId && message.roomId !== roomId) {
        return;
      }
      switch (message.type) {
        case "ROOM_STATE":
          setRoom(message.payload as RoomState);
          break;
        case "COMMAND_RESULT":
          setLastResult(message.payload as CommandResult);
          setError(null);
          break;
        case "ERROR":
          setError(message.payload as ErrorPayload);
          break;
      }
    });
    client.joinRoom(roomId);
    client.connect();
    return () => {
      stopState();
      stopMessages();
      client.disconnect();
    };
  }, [client, roomId]);

  const sendCommand = useCallback(
    (command: RoomCommand) => {
      lastCommand.current = command;
      if (!client.send("COMMAND", command)) {
        setError({ code: "NOT_CONNECTED", message: "Sem conexão com o servidor; tente quando reconectar" });
      }
    },
    [client],
  );

  const increment = useCallback(
    () => sendCommand({ commandId: newId(), type: "INCREMENT_COUNTER", payload: { amount: 1 } }),
    [sendCommand],
  );

  /** Reenvia o último comando com o mesmo commandId, para demonstrar a idempotência. */
  const resendLast = useCallback(() => {
    if (lastCommand.current) {
      sendCommand(lastCommand.current);
    }
  }, [sendCommand]);

  return { connection, room, lastResult, error, increment, resendLast, canResend: lastResult !== null };
}
