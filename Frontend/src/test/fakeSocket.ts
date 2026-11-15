import type { SocketFactory } from "../lib/realtimeClient";

/** WebSocket falso e controlável para testes. */
export class FakeSocket {
  static instances: FakeSocket[] = [];

  readyState = 0;
  sent: Array<{ type: string; id: string; payload?: unknown }> = [];
  onopen: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  readonly url: string;

  constructor(url: string) {
    this.url = url;
    FakeSocket.instances.push(this);
  }

  send(data: string) {
    this.sent.push(JSON.parse(data));
  }

  close() {
    this.readyState = 3;
    this.onclose?.({} as CloseEvent);
  }

  serverOpen() {
    this.readyState = 1;
    this.onopen?.({} as Event);
  }

  serverSend(message: unknown) {
    this.onmessage?.({ data: typeof message === "string" ? message : JSON.stringify(message) } as MessageEvent);
  }

  serverDrop() {
    this.readyState = 3;
    this.onclose?.({} as CloseEvent);
  }

  static latest(): FakeSocket {
    const socket = FakeSocket.instances.at(-1);
    if (!socket) {
      throw new Error("Nenhum socket criado");
    }
    return socket;
  }

  static reset() {
    FakeSocket.instances = [];
  }
}

export const fakeSocketFactory: SocketFactory = (url) => new FakeSocket(url);

