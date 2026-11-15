import type { MatchEvent, MatchView } from "./types";

type Props = {
  events: MatchEvent[];
  match: MatchView | null;
};

/** Histórico da partida, do mais antigo ao mais recente. */
export function MatchHistory({ events, match }: Props) {
  const nationName = new Map(match?.nations.map((nation) => [nation.id, nation.name]));

  function describe(event: MatchEvent): string {
    const who = event.playerName ?? event.playerId ?? "Alguém";
    switch (event.type) {
      case "MATCH_CREATED":
        return `${who} criou a partida`;
      case "PLAYER_JOINED": {
        const nation = String(event.data.nationId ?? "");
        return `${who} entrou com ${nationName.get(nation) ?? nation}`;
      }
      case "MATCH_STARTED":
        return `${who} iniciou a partida`;
      default:
        return event.type;
    }
  }

  return (
    <section className="match-history" aria-labelledby="history-title">
      <h3 id="history-title">Histórico</h3>
      {events.length === 0 ? (
        <p className="muted">Nada registrado ainda.</p>
      ) : (
        <ol>
          {events.map((event) => (
            <li key={event.seq}>
              <time dateTime={event.occurredAt} className="muted">
                {new Date(event.occurredAt).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}
              </time>{" "}
              {describe(event)}
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
