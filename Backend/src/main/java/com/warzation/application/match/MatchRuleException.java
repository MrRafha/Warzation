package com.warzation.application.match;

/** Ação recusada por uma regra da partida (nação ocupada, partida cheia...). */
public class MatchRuleException extends RuntimeException {

    public enum Reason {
        NATION_TAKEN,
        MATCH_FULL,
        MATCH_NOT_OPEN,
        ALREADY_JOINED,
        NOT_ENOUGH_PLAYERS,
        /** Ação reservada a quem criou a partida (403). */
        NOT_MATCH_CREATOR,
        /** Ação ou leitura reservada a participantes (403). */
        NOT_PARTICIPANT;

        public boolean isForbidden() {
            return this == NOT_MATCH_CREATOR || this == NOT_PARTICIPANT;
        }
    }

    private final Reason reason;

    public MatchRuleException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
