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
            Map<String, Long> stock,
            List<Building> buildings,
            Economy economy) {
    }

    /** Construção na província; {@code completedAt} nulo enquanto está em obra. */
    public record Building(
            String id,
            String type,
            String status,
            OffsetDateTime startedAt,
            OffsetDateTime completesAt,
            OffsetDateTime completedAt) {
    }

    /**
     * Economia por ciclo, calculada com as construções em funcionamento agora.
     * Nula para províncias neutras. {@code foodBalance} = produção de comida
     * menos demanda; {@code foodShortageLastCycle} &gt; 0 indica déficit.
     */
    public record Economy(
            Map<String, Long> production,
            long foodDemand,
            long foodCapacity,
            long foodBalance,
            long foodShortageLastCycle,
            long foodShortageTotal,
            boolean inFoodDeficit) {
    }

    /** Dono nulo = guarnição neutra. */
    public record Units(String type, int quantity, String ownerNationId) {
    }
}
