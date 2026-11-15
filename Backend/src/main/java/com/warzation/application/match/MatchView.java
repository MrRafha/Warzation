package com.warzation.application.match;

import java.time.OffsetDateTime;
import java.util.List;

/** Estado público de uma partida. */
public record MatchView(
        String id,
        String name,
        String scenarioId,
        String status,
        int maxPlayers,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        Economy economy,
        List<Participant> participants,
        List<Nation> nations) {

    /**
     * Relógio econômico; nulo antes de a partida começar. {@code skippedCycles}
     * conta ciclos descartados por excederem a recuperação após o servidor
     * ficar desligado.
     */
    public record Economy(long cycles, long skippedCycles, OffsetDateTime nextCycleAt) {
    }

    public record Participant(String playerId, String playerName, String nationId, OffsetDateTime joinedAt) {
    }

    /** Nação do cenário e, se já escolhida, o jogador que a controla. */
    public record Nation(String id, String name, String capital, String playerId) {
    }
}
