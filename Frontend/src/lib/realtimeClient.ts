/**
 * Cliente WebSocket do Warzation. O contrato das mensagens está em
 * `Docs/03_ARQUITETURA_E_STACK.md`, seção "Contrato do WebSocket".
 *
 * O cliente nunca guarda estado de jogo como verdade: ao reconectar, ele
 * volta para a sala e aguarda o ROOM_STATE oficial enviado pelo servidor.
 */

export type ServerMessage = {
  type: string;
  id: string;
  replyTo?: string;
  roomId?: string;
  payload?: unknown;
};

export type ConnectionState = "connecting" | "open" | "reconnecting" | "closed";

type Listener<T> = (value: T) => void;

type SocketLike = {
  readyState: number;
  onopen: ((event: Event) => void) | null;
  onclose: ((event: CloseEvent) => void) | null;
  onerror: ((event: Event) => void) | null;
  onmessage: ((event: MessageEvent) => void) | null;
  send(data: string): void;
  close(code?: number, reason?: string): void;
};

export type SocketFactory = (url: string) => SocketLike;

const OPEN = 1;
const INITIAL_RETRY_MS = 1_000;
const MAX_RETRY_MS = 10_000;

export class RealtimeClient {
  private readonly url: string;
  private readonly createSocket: SocketFactory;
  private readonly token: string | null;
  private socket: SocketLike | null = null;
  private roomId: string | null = null;
  private retryMs = INITIAL_RETRY_MS;
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  private stopped = false;
  private state: ConnectionState = "closed";
  private readonly messageListeners = new Set<Listener<ServerMessage>>();
  private readonly stateListeners = new Set<Listener<ConnectionState>>();

  /**
   * @param token token do jogador; enviado em AUTHENTICATE a cada conexão,
   *   antes de entrar na sala. O servidor processa as mensagens em ordem.
   */
  constructor(
    url: string,
    createSocket: SocketFactory = (target) => new WebSocket(target),
    token: string | null = null,
  ) {
    this.url = url;
    this.createSocket = createSocket;
    this.token = token;
  }

  connect(): void {
    this.stopped = false;
    this.open("connecting");
  }

  disconnect(): void {
    this.stopped = true;
    if (this.retryTimer) {
      clearTimeout(this.retryTimer);
      this.retryTimer = null;
    }
    const socket = this.socket;
    this.socket = null;
    socket?.close(1000, "cliente encerrou");
    this.setState("closed");
  }

  /** Entra na sala agora (se conectado) e de novo a cada reconexão. */
  joinRoom(roomId: string): string | null {
    this.roomId = roomId;
    return this.send("JOIN_ROOM", { roomId });
  }

  /** Envia uma mensagem e devolve seu id, ou null se o socket não estiver aberto. */
  send(type: string, payload?: unknown): string | null {
    if (!this.socket || this.socket.readyState !== OPEN) {
      return null;
    }
    const id = newId();
    this.socket.send(JSON.stringify(payload === undefined ? { type, id } : { type, id, payload }));
    return id;
  }

  onMessage(listener: Listener<ServerMessage>): () => void {
    this.messageListeners.add(listener);
    return () => this.messageListeners.delete(listener);
  }

  onStateChange(listener: Listener<ConnectionState>): () => void {
    this.stateListeners.add(listener);
    listener(this.state);
    return () => this.stateListeners.delete(listener);
  }

  getState(): ConnectionState {
    return this.state;
  }

  private open(state: ConnectionState): void {
    this.setState(state);
    const socket = this.createSocket(this.url);
    this.socket = socket;

    socket.onopen = () => {
      this.retryMs = INITIAL_RETRY_MS;
      this.setState("open");
      if (this.token) {
        this.send("AUTHENTICATE", { token: this.token });
      }
      if (this.roomId) {
        this.send("JOIN_ROOM", { roomId: this.roomId });
      }
    };

    socket.onmessage = (event) => {
      const message = parseMessage(event.data);
      if (!message) {
        console.warn("[realtime] mensagem inválida ignorada", event.data);
        return;
      }
      this.messageListeners.forEach((listener) => listener(message));
    };

    socket.onclose = () => {
      if (this.socket !== socket) {
        return;
      }
      this.socket = null;
      if (this.stopped) {
        this.setState("closed");
        return;
      }
      this.scheduleReconnect();
    };

    socket.onerror = () => {
      // o evento close sempre vem em seguida e cuida da reconexão
    };
  }

  private scheduleReconnect(): void {
    this.setState("reconnecting");
    const delay = this.retryMs;
    this.retryMs = Math.min(this.retryMs * 2, MAX_RETRY_MS);
    this.retryTimer = setTimeout(() => {
      this.retryTimer = null;
      if (!this.stopped) {
        this.open("reconnecting");
      }
    }, delay);
  }

  private setState(state: ConnectionState): void {
    if (this.state === state) {
      return;
    }
    this.state = state;
    this.stateListeners.forEach((listener) => listener(state));
  }
}

export function parseMessage(data: unknown): ServerMessage | null {
  if (typeof data !== "string") {
    return null;
  }
  try {
    const value: unknown = JSON.parse(data);
    if (
      typeof value === "object" &&
      value !== null &&
      typeof (value as ServerMessage).type === "string" &&
      typeof (value as ServerMessage).id === "string"
    ) {
      return value as ServerMessage;
    }
  } catch {
    // cai no retorno nulo abaixo
  }
  return null;
}

export function newId(): string {
  return crypto.randomUUID();
}
