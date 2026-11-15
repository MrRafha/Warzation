export type AppConfig = {
  apiBaseUrl: string;
  wsUrl: string;
};

export type ConfigResult =
  | { ok: true; config: AppConfig }
  | { ok: false; missing: string[] };

type Env = Record<string, string | boolean | undefined>;

/**
 * Lê as URLs do Backend das variáveis do Vite. Não há valor padrão no código:
 * sem `.env`, a interface avisa o que falta em vez de tentar um endereço fixo.
 */
export function readConfig(env: Env = import.meta.env): ConfigResult {
  const apiBaseUrl = readString(env.VITE_API_BASE_URL);
  const wsUrl = readString(env.VITE_WS_URL);
  const missing = [
    ...(apiBaseUrl ? [] : ["VITE_API_BASE_URL"]),
    ...(wsUrl ? [] : ["VITE_WS_URL"]),
  ];

  if (!apiBaseUrl || !wsUrl) {
    return { ok: false, missing };
  }
  return { ok: true, config: { apiBaseUrl: apiBaseUrl.replace(/\/+$/, ""), wsUrl } };
}

function readString(value: string | boolean | undefined): string | undefined {
  return typeof value === "string" && value.trim() !== "" ? value.trim() : undefined;
}
