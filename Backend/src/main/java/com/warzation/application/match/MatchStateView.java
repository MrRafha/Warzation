package com.warzation.application.match;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Estado territorial oficial da partida. Antes do início, a lista de províncias é vazia. */
public record MatchStateView(
        String matchId,
        String status,
        OffsetDateTime startedAt,
        List<Province> provinces) {

    /** Dono nulo = província neutra. */
    public record Province(
            String provinceId,
            String ownerNationId,
            String ownerPlayerId,
            List<Units> units,
            Map<String, Long> stock) {
    }

    /** Dono nulo = guarnição neutra. */
    public record Units(String type, int quantity, String ownerNationId) {
    }
}
