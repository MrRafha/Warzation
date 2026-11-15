export type ApiErrorBody = {
  code?: string;
  message?: string;
  fields?: Record<string, string>;
};

export type ApiErrorKind = "network" | "timeout" | "http" | "invalid-response";

export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status?: number;
  readonly body?: ApiErrorBody;

  constructor(kind: ApiErrorKind, message: string, status?: number, body?: ApiErrorBody) {
    super(message);
    this.name = "ApiError";
    this.kind = kind;
    this.status = status;
    this.body = body;
  }
}

export type ApiClient = {
  get<T>(path: string, options?: RequestOptions): Promise<T>;
  post<T>(path: string, body: unknown, options?: RequestOptions): Promise<T>;
};

export type RequestOptions = {
  signal?: AbortSignal;
  timeoutMs?: number;
};

const DEFAULT_TIMEOUT_MS = 5_000;

/** Cliente HTTP único da aplicação: base URL, timeout e erros padronizados. */
export function createApiClient(baseUrl: string, fetchImpl: typeof fetch = fetch): ApiClient {
  async function request<T>(method: string, path: string, body: unknown, options: RequestOptions = {}) {
    const controller = new AbortController();
    let timedOut = false;
    const timeout = setTimeout(() => {
      timedOut = true;
      controller.abort();
    }, options.timeoutMs ?? DEFAULT_TIMEOUT_MS);
    const abortFromCaller = () => controller.abort();
    options.signal?.addEventListener("abort", abortFromCaller);

    let response: Response;
    try {
      response = await fetchImpl(`${baseUrl}${path}`, {
        method,
        signal: controller.signal,
        headers: body === undefined ? { Accept: "application/json" } : {
          Accept: "application/json",
          "Content-Type": "application/json",
        },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch (error) {
      if (timedOut) {
        throw new ApiError("timeout", "O Backend demorou demais para responder");
      }
      if (options.signal?.aborted) {
        throw error;
      }
      throw new ApiError("network", "Não foi possível alcançar o Backend");
    } finally {
      clearTimeout(timeout);
      options.signal?.removeEventListener("abort", abortFromCaller);
    }

    const payload = await readJson(response);
    if (!response.ok) {
      const errorBody = isObject(payload) ? (payload as ApiErrorBody) : undefined;
      throw new ApiError(
        "http",
        errorBody?.message ?? `O Backend respondeu ${response.status}`,
        response.status,
        errorBody,
      );
    }
    if (payload === undefined) {
      throw new ApiError("invalid-response", "Resposta do Backend sem JSON válido", response.status);
    }
    return payload as T;
  }

  return {
    get: (path, options) => request("GET", path, undefined, options),
    post: (path, body, options) => request("POST", path, body, options),
  };
}

async function readJson(response: Response): Promise<unknown> {
  try {
    return await response.json();
  } catch {
    return undefined;
  }
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
