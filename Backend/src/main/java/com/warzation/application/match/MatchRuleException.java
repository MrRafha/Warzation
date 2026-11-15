package com.warzation.application.match;

/** Ação recusada por uma regra da partida (nação ocupada, partida cheia...). */
public class MatchRuleException extends RuntimeException {

    public enum Reason {
        NATION_TAKEN,
        MATCH_FULL,
        MATCH_NOT_OPEN,
        ALREADY_JOINED
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
