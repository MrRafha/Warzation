package com.warzation.application.rules;

import java.util.List;
import java.util.Map;

/**
 * Formato de Backend/data/rules/game-rules.json. Campos numéricos são objetos
 * (Integer/Double) para que a ausência seja detectada pelo validador em vez de
 * virar zero silenciosamente.
 */
public record GameRulesFile(
        Integer formatVersion,
        Boolean provisional,
        Economy economy,
        List<Building> buildings) {

    public record Economy(
            Integer cycleMinutes,
            Integer maxCatchUpCycles,
            Integer foodDemandPerProvince,
            Integer baseFoodCapacity,
            Integer cancelRefundPercent,
            Map<String, Double> abundanceMultipliers) {
    }

    public record Building(
            String id,
            String name,
            String era,
            String requiredTechnology,
            Map<String, Integer> cost,
            Integer buildMinutes,
            Production production,
            Extraction extraction,
            Integer foodCapacityBonus) {
    }

    /** Produção que não depende de depósito, modulada pelo terreno. */
    public record Production(String resource, Integer perCycle, Map<String, Double> terrainMultipliers) {
    }

    /** Extração que exige um depósito de um dos recursos na província, modulada pela abundância. */
    public record Extraction(List<String> resources, Integer perCycle) {
    }
}
