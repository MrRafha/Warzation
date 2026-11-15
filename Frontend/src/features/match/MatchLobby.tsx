import { useCallback, useEffect, useState, type FormEvent } from "react";
import { ApiError, type ApiClient } from "../../lib/apiClient";
import { DEFAULT_SCENARIO_ID } from "../map/scenario";
import { createMatch, joinMatch, listOpenMatches } from "./matchApi";
import type { MatchView } from "./types";

type Props = {
  api: ApiClient;
  playerId: string | null;
};

/**
 * Lobby provisório: lista partidas abertas, cria e entra escolhendo uma nação.
 * Atualiza ao agir ou no botão; a sincronização em tempo real vem com a #20.
 */
export function MatchLobby({ api, playerId }: Props) {
  const [matches, setMatches] = useState<MatchView[] | null>(null);
  const [listError, setListError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [name, setName] = useState("");

  const refresh = useCallback(async () => {
    try {
      setMatches(await listOpenMatches(api));
      setListError(null);
    } catch (caught) {
      setListError(message(caught));
    }
  }, [api]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    try {
      await action();
      setActionError(null);
    } catch (caught) {
      setActionError(message(caught));
    } finally {
      setBusy(false);
      await refresh();
    }
  }

  function submitCreate(event: FormEvent) {
    event.preventDefault();
    void run(async () => {
      await createMatch(api, DEFAULT_SCENARIO_ID, name);
      setName("");
    });
  }

  return (
    <section className="match-lobby" aria-labelledby="lobby-title">
      <div className="lobby-header">
        <h2 id="lobby-title">Partidas abertas</h2>
        <button type="button" className="link-button" onClick={() => void refresh()} disabled={busy}>
          Atualizar
        </button>
      </div>

      {matches === null && !listError && <p className="muted">Carregando partidas...</p>}
      {matches?.length === 0 && <p className="muted">Nenhuma partida aberta.</p>}

      <ul className="match-list">
        {matches?.map((match) => {
          const mine = match.participants.find((p) => p.playerId === playerId);
          return (
            <li key={match.id} className="match-card">
              <p className="match-name">
                <strong>{match.name}</strong>
                <span className="muted">
                  {" "}
                  · {match.participants.length}/{match.maxPlayers} jogadores
                </span>
              </p>
              <ul className="nation-slots">
                {match.nations.map((nation) => {
                  const holder = match.participants.find((p) => p.nationId === nation.id);
                  return (
                    <li key={nation.id}>
                      <span>{nation.name}</span>
                      {holder ? (
                        <span className="muted">{holder.playerId === playerId ? "você" : holder.playerName}</span>
                      ) : playerId && !mine ? (
                        <button
                          type="button"
                          className="link-button"
                          disabled={busy}
                          aria-label={`Entrar com ${nation.name}`}
                          onClick={() => void run(() => joinMatch(api, match.id, nation.id))}
                        >
                          Entrar
                        </button>
                      ) : (
                        <span className="muted">livre</span>
                      )}
                    </li>
                  );
                })}
              </ul>
              {mine && <p className="match-mine">Você está nesta partida.</p>}
            </li>
          );
        })}
      </ul>

      {playerId ? (
        <form className="lobby-create" onSubmit={submitCreate}>
          <input
            aria-label="Nome da nova partida"
            placeholder="Nome da partida (opcional)"
            maxLength={60}
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
          <button type="submit" disabled={busy}>
            Criar partida
          </button>
        </form>
      ) : (
        <p className="muted">Entre como jogador de teste para criar ou entrar em partidas.</p>
      )}

      {[actionError, listError].filter(Boolean).map((text) => (
        <p key={text} className="lobby-error" role="alert">
          {text}
        </p>
      ))}
    </section>
  );
}

function message(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return "Erro inesperado";
}
