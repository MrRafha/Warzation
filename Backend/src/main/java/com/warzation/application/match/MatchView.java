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
        List<Participant> participants,
        List<Nation> nations) {

    public record Participant(String playerId, String playerName, String nationId, OffsetDateTime joinedAt) {
    }

    /** Nação do cenário e, se já escolhida, o jogador que a controla. */
    public record Nation(String id, String name, String capital, String playerId) {
    }
}
