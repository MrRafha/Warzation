import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { FakeSocket, fakeSocketFactory } from "../test/fakeSocket";
import { parseMessage, RealtimeClient, type ServerMessage } from "./realtimeClient";

beforeEach(() => {
  FakeSocket.reset();
  vi.useFakeTimers();
});

afterEach(() => {
  vi.useRealTimers();
});

describe("RealtimeClient", () => {
  it("entra na sala assim que conecta", () => {
    const client = new RealtimeClient("ws://test/ws", fakeSocketFactory);
    client.joinRoom("dev");
    client.connect();

    const socket = FakeSocket.latest();
    expect(socket.url).toBe("ws://test/ws");
    socket.serverOpen();

    expect(client.getState()).toBe("open");
    expect(socket.sent).toEqual([expect.objectContaining({ type: "JOIN_ROOM", payload: { roomId: "dev" } })]);
  });

  it("repassa mensagens válidas e ignora as inválidas", () => {
    const client = new RealtimeClient("ws://test/ws", fakeSocketFactory);
    const received: ServerMessage[] = [];
    client.onMessage((message) => received.push(message));
    client.connect();
    const socket = FakeSocket.latest();
    socket.serverOpen();
    vi.spyOn(console, "warn").mockImplementation(() => {});

    socket.serverSend("{ quebrado");
    socket.serverSend({ semTipo: true });
    socket.serverSend({ type: "ROOM_STATE", id: "m1", roomId: "dev", payload: { counter: 1 } });

    expect(received).toEqual([{ type: "ROOM_STATE", id: "m1", roomId: "dev", payload: { counter: 1 } }]);
  });

  it("reconecta com espera crescente e volta para a sala", () => {
    const client = new RealtimeClient("ws://test/ws", fakeSocketFactory);
    const states: string[] = [];
    client.onStateChange((state) => states.push(state));
    client.joinRoom("dev");
    client.connect();
    FakeSocket.latest().serverOpen();

    FakeSocket.latest().serverDrop();
    expect(client.getState()).toBe("reconnecting");
    vi.advanceTimersByTime(999);
    expect(FakeSocket.instances).toHaveLength(1);
    vi.advanceTimersByTime(1);
    expect(FakeSocket.instances).toHaveLength(2);

    FakeSocket.latest().serverDrop();
    vi.advanceTimersByTime(1999);
    expect(FakeSocket.instances).toHaveLength(2);
    vi.advanceTimersByTime(1);
    expect(FakeSocket.instances).toHaveLength(3);

    const socket = FakeSocket.latest();
    socket.serverOpen();
    expect(socket.sent).toEqual([expect.objectContaining({ type: "JOIN_ROOM", payload: { roomId: "dev" } })]);
    expect(states).toEqual(["closed", "connecting", "open", "reconnecting", "open"]);
  });

  it("não reconecta depois de desconectar de propósito", () => {
    const client = new RealtimeClient("ws://test/ws", fakeSocketFactory);
    client.connect();
    FakeSocket.latest().serverOpen();

    client.disconnect();
    vi.advanceTimersByTime(20_000);

    expect(client.getState()).toBe("closed");
    expect(FakeSocket.instances).toHaveLength(1);
  });

  it("não envia nada com o socket fechado", () => {
    const client = new RealtimeClient("ws://test/ws", fakeSocketFactory);
    client.connect();

    expect(client.send("PING")).toBeNull();
    expect(FakeSocket.latest().sent).toEqual([]);
  });
});

describe("parseMessage", () => {
  it("exige type e id", () => {
    expect(parseMessage('{"type":"PONG","id":"1"}')).toEqual({ type: "PONG", id: "1" });
    expect(parseMessage('{"type":"PONG"}')).toBeNull();
    expect(parseMessage(42)).toBeNull();
  });
});
