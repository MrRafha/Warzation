import { useState, type FormEvent } from "react";
import type { PlayerSession } from "./usePlayerSession";

type Props = {
  session: PlayerSession;
  onLogin: (token: string) => void;
  onLogout: () => void;
};

/** Entrada provisória do protótipo: jogador de teste por token. Não é um sistema de contas. */
export function PlayerBar({ session, onLogin, onLogout }: Props) {
  const [token, setToken] = useState("");

  if (session.status === "authenticated") {
    return (
      <p className="player-bar">
        Jogando como <strong>{session.player.name}</strong>{" "}
        <button type="button" className="link-button" onClick={onLogout}>
          Sair
        </button>
      </p>
    );
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (token.trim()) {
      onLogin(token);
      setToken("");
    }
  }

  return (
    <form className="player-bar" onSubmit={submit}>
      <input
        type="password"
        autoComplete="off"
        placeholder="Token do jogador de teste"
        aria-label="Token do jogador de teste"
        value={token}
        onChange={(event) => setToken(event.target.value)}
        disabled={session.status === "checking"}
      />
      <button type="submit" disabled={session.status === "checking" || !token.trim()}>
        {session.status === "checking" ? "Verificando..." : "Entrar"}
      </button>
      {session.status === "anonymous" && session.error && (
        <span className="player-error" role="alert">
          {session.error}
        </span>
      )}
    </form>
  );
}
