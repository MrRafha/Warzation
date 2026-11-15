import { useCallback, useEffect, useState } from "react";
import { ApiError, type ApiClient } from "../../lib/apiClient";
import { clearStoredToken, readStoredToken, storeToken } from "../../lib/playerSession";

export type Player = { id: string; name: string };

export type PlayerSession =
  | { status: "checking" }
  | { status: "anonymous"; error?: string }
  | { status: "authenticated"; player: Player; token: string };

/** Identidade do jogador de teste nesta aba, confirmada pelo servidor em /api/v1/players/me. */
export function usePlayerSession(api: ApiClient) {
  const [session, setSession] = useState<PlayerSession>(() =>
    readStoredToken() ? { status: "checking" } : { status: "anonymous" },
  );

  const verify = useCallback(
    async (token: string) => {
      setSession({ status: "checking" });
      try {
        const player = await api.get<Player>("/api/v1/players/me", { token });
        storeToken(token);
        setSession({ status: "authenticated", player, token });
      } catch (error) {
        clearStoredToken();
        setSession({
          status: "anonymous",
          error:
            error instanceof ApiError && error.status === 401
              ? "Token não reconhecido pelo servidor"
              : "Não foi possível confirmar o jogador agora",
        });
      }
    },
    [api],
  );

  useEffect(() => {
    const stored = readStoredToken();
    if (stored) {
      void verify(stored);
    }
  }, [verify]);

  const login = useCallback((token: string) => verify(token.trim()), [verify]);

  const logout = useCallback(() => {
    clearStoredToken();
    setSession({ status: "anonymous" });
  }, []);

  return { session, login, logout };
}
