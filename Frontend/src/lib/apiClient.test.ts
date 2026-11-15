import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, createApiClient } from "./apiClient";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

afterEach(() => {
  vi.useRealTimers();
});

describe("createApiClient", () => {
  it("monta a URL a partir da base configurada e devolve o JSON", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, { status: "UP" }));
    const api = createApiClient("http://api.test", fetchMock);

    await expect(api.get("/api/v1/health")).resolves.toEqual({ status: "UP" });
    expect(fetchMock).toHaveBeenCalledWith("http://api.test/api/v1/health", expect.objectContaining({ method: "GET" }));
  });

  it("envia JSON no POST", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(201, { ok: true }));
    const api = createApiClient("http://api.test", fetchMock);

    await api.post("/api/v1/metadata", { key: "a", value: "b" });
    const [, init] = fetchMock.mock.calls[0];
    expect(init.body).toBe(JSON.stringify({ key: "a", value: "b" }));
    expect(init.headers["Content-Type"]).toBe("application/json");
  });

  it("envia o token do jogador quando houver", async () => {
    const fetchMock = vi.fn().mockImplementation(async () => jsonResponse(200, {}));
    let token: string | null = null;
    const api = createApiClient("http://api.test", fetchMock, () => token);

    await api.get("/sem-token");
    token = "abc-123";
    await api.get("/com-token");
    await api.get("/outro", { token: "token-explicito" });

    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBeUndefined();
    expect(fetchMock.mock.calls[1][1].headers.Authorization).toBe("Bearer abc-123");
    expect(fetchMock.mock.calls[2][1].headers.Authorization).toBe("Bearer token-explicito");
  });

  it("converte erros HTTP no formato padrão da API", async () => {
    const api = createApiClient(
      "http://api.test",
      vi.fn().mockResolvedValue(jsonResponse(400, { code: "VALIDATION_ERROR", message: "Dados inválidos" })),
    );

    const error = await api.get("/x").catch((caught: unknown) => caught);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ kind: "http", status: 400, message: "Dados inválidos", body: { code: "VALIDATION_ERROR" } });
  });

  it("indica falha de rede quando o Backend está fora do ar", async () => {
    const api = createApiClient("http://api.test", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));

    await expect(api.get("/x")).rejects.toMatchObject({ kind: "network" });
  });

  it("indica timeout quando o Backend não responde", async () => {
    vi.useFakeTimers();
    const neverResolves = vi.fn(
      (_url: string, init: RequestInit) =>
        new Promise<Response>((_resolve, reject) => {
          init.signal?.addEventListener("abort", () => reject(new DOMException("aborted", "AbortError")));
        }),
    );
    const api = createApiClient("http://api.test", neverResolves as unknown as typeof fetch);

    const pending = api.get("/x", { timeoutMs: 1000 });
    vi.advanceTimersByTime(1000);
    await expect(pending).rejects.toMatchObject({ kind: "timeout" });
  });

  it("rejeita resposta de sucesso sem JSON", async () => {
    const api = createApiClient("http://api.test", vi.fn().mockResolvedValue(new Response("ok", { status: 200 })));

    await expect(api.get("/x")).rejects.toMatchObject({ kind: "invalid-response" });
  });
});
