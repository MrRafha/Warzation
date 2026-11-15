package com.warzation.application.match;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MatchEventView(
        int seq,
        String type,
        String playerId,
        String playerName,
        UUID commandId,
        OffsetDateTime occurredAt,
        JsonNode data) {
}
