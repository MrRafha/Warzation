import { describe, expect, it } from "vitest";
import { readConfig } from "./config";

describe("readConfig", () => {
  it("lê as URLs das variáveis de ambiente", () => {
    expect(
      readConfig({ VITE_API_BASE_URL: "http://localhost:8080/", VITE_WS_URL: "ws://localhost:8080/ws" }),
    ).toEqual({
      ok: true,
      config: { apiBaseUrl: "http://localhost:8080", wsUrl: "ws://localhost:8080/ws" },
    });
  });

  it("aponta as variáveis ausentes em vez de usar um endereço fixo", () => {
    expect(readConfig({})).toEqual({ ok: false, missing: ["VITE_API_BASE_URL", "VITE_WS_URL"] });
    expect(readConfig({ VITE_API_BASE_URL: "  ", VITE_WS_URL: "ws://x/ws" })).toEqual({
      ok: false,
      missing: ["VITE_API_BASE_URL"],
    });
  });
});
