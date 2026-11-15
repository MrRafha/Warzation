import { useCallback, useEffect, useState } from "react";
import { ApiError, type ApiClient } from "../../lib/apiClient";

export type HealthResponse = {
  status: string;
  service: string;
  version: string;
};

export type BackendHealth =
  | { status: "checking" }
  | { status: "online"; info: HealthResponse }
  | { status: "offline"; reason: string };

/** Consulta `/api/v1/health` e expõe um estado de conexão para a interface. */
export function useBackendHealth(api: ApiClient) {
  const [health, setHealth] = useState<BackendHealth>({ status: "checking" });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setHealth({ status: "checking" });

    api
      .get<HealthResponse>("/api/v1/health", { signal: controller.signal })
      .then((info) => {
        setHealth(
          info.status === "UP"
            ? { status: "online", info }
            : { status: "offline", reason: `Backend respondeu status ${info.status}` },
        );
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) {
          return;
        }
        setHealth({
          status: "offline",
          reason: error instanceof ApiError ? error.message : "Erro inesperado ao consultar o Backend",
        });
      });

    return () => controller.abort();
  }, [api, attempt]);

  const retry = useCallback(() => setAttempt((value) => value + 1), []);
  return { health, retry };
}
