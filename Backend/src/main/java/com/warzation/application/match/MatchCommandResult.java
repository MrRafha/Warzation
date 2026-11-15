package com.warzation.application.match;

import java.util.UUID;

/** {@code APPLIED} na primeira execução; {@code DUPLICATE} quando o mesmo commandId é reenviado. */
public record MatchCommandResult(UUID commandId, Status status, MatchView match) {

    public enum Status {
        APPLIED,
        DUPLICATE
    }
}
