package com.warzation.application.rules;

import com.warzation.application.map.ScenarioFile;
import com.warzation.config.MatchRulesProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Verifica as regras de jogo e devolve todos os problemas de uma vez. */
public final class GameRulesValidator {

    /** Nenhuma construção ou recrutamento pode levar mais de 24 h (Docs/01, seção 10). */
    public static final int MAX_BUILD_MINUTES = 24 * 60;

    static final Set<String> ERAS = Set.of("ANTIGA", "MEDIEVAL", "INDUSTRIAL", "MODERNA");
    private static final Pattern BUILDING_ID = Pattern.compile("[A-Z][A-Z0-9_]*");
    private static final Pattern TECHNOLOGY_ID = Pattern.compile("[a-z][a-z0-9-]*");

    private GameRulesValidator() {
    }

    /**
     * @param knownTerrains terrenos declarados nos cenários carregados
     */
    public static List<String> validate(GameRulesFile file, Set<String> knownTerrains) {
        List<String> problems = new ArrayList<>();
        if (!Integer.valueOf(1).equals(file.formatVersion())) {
            problems.add("formatVersion precisa ser 1");
        }
        if (file.provisional() == null) {
            problems.add("provisional é obrigatório (true enquanto os valores forem de teste)");
        }
        validateEconomy(file.economy(), problems);
        validateBuildings(file.buildings(), knownTerrains, problems);
        return problems;
    }

    private static void validateEconomy(GameRulesFile.Economy economy, List<String> problems) {
        if (economy == null) {
            problems.add("economy é obrigatório");
            return;
        }
        atLeast("economy.cycleMinutes", economy.cycleMinutes(), 1, problems);
        atLeast("economy.maxCatchUpCycles", economy.maxCatchUpCycles(), 1, problems);
        atLeast("economy.foodDemandPerProvince", economy.foodDemandPerProvince(), 0, problems);
        atLeast("economy.baseFoodCapacity", economy.baseFoodCapacity(), 1, problems);
        if (economy.cancelRefundPercent() == null
                || economy.cancelRefundPercent() < 0 || economy.cancelRefundPercent() > 100) {
            problems.add("economy.cancelRefundPercent precisa estar entre 0 e 100");
        }
        Set<String> abundances = Arrays.stream(ScenarioFile.Abundance.values()).map(Enum::name)
                .collect(Collectors.toSet());
        Map<String, Double> multipliers = economy.abundanceMultipliers();
        if (multipliers == null || !multipliers.keySet().equals(abundances)) {
            problems.add("economy.abundanceMultipliers precisa ter exatamente " + sorted(abundances));
        } else {
            multipliers.forEach((key, value) -> positive("economy.abundanceMultipliers." + key, value, problems));
        }
    }

    private static void validateBuildings(List<GameRulesFile.Building> buildings, Set<String> knownTerrains,
            List<String> problems) {
        if (buildings == null || buildings.isEmpty()) {
            problems.add("buildings precisa ter pelo menos uma construção");
            return;
        }
        Set<String> depositResources = Arrays.stream(ScenarioFile.Resource.values()).map(Enum::name)
                .collect(Collectors.toSet());
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < buildings.size(); i++) {
            GameRulesFile.Building building = buildings.get(i);
            String at = "buildings[" + i + "]";
            if (building == null) {
                problems.add(at + " não pode ser nulo");
                continue;
            }
            if (building.id() == null || !BUILDING_ID.matcher(building.id()).matches()) {
                problems.add(at + ".id precisa ser MAIUSCULO_COM_SUBLINHADO");
            } else {
                at = "construção " + building.id();
                if (!ids.add(building.id())) {
                    problems.add("id de construção repetido: " + building.id());
                }
            }
            if (building.name() == null || building.name().isBlank()) {
                problems.add(at + ": name é obrigatório");
            }
            if (!ERAS.contains(building.era())) {
                problems.add(at + ": era desconhecida (" + building.era() + "); use " + sorted(ERAS));
            }
            if (building.requiredTechnology() == null
                    || !TECHNOLOGY_ID.matcher(building.requiredTechnology()).matches()) {
                problems.add(at + ": requiredTechnology precisa ser minúsculo-com-hífen");
            }
            if (building.buildMinutes() == null || building.buildMinutes() < 1
                    || building.buildMinutes() > MAX_BUILD_MINUTES) {
                problems.add(at + ": buildMinutes precisa estar entre 1 e " + MAX_BUILD_MINUTES + " (24 h)");
            }
            validateCost(at, building.cost(), problems);
            if (building.production() != null) {
                validateProduction(at, building.production(), knownTerrains, problems);
            }
            if (building.extraction() != null) {
                validateExtraction(at, building.extraction(), depositResources, problems);
            }
            if (building.foodCapacityBonus() != null && building.foodCapacityBonus() < 1) {
                problems.add(at + ": foodCapacityBonus precisa ser positivo");
            }
            if (building.production() != null && building.extraction() != null) {
                problems.add(at + ": use production ou extraction, não os dois");
            }
        }
    }

    private static void validateCost(String at, Map<String, Integer> cost, List<String> problems) {
        if (cost == null || cost.isEmpty()) {
            problems.add(at + ": cost precisa ter pelo menos um recurso");
            return;
        }
        cost.forEach((resource, amount) -> {
            if (!MatchRulesProperties.RESOURCES.contains(resource)) {
                problems.add(at + ": recurso desconhecido no custo: " + resource);
            }
            if (amount == null || amount < 1) {
                problems.add(at + ": custo de " + resource + " precisa ser positivo");
            }
        });
    }

    private static void validateProduction(String at, GameRulesFile.Production production, Set<String> knownTerrains,
            List<String> problems) {
        if (!MatchRulesProperties.RESOURCES.contains(production.resource())) {
            problems.add(at + ": recurso produzido desconhecido: " + production.resource());
        }
        atLeast(at + ": production.perCycle", production.perCycle(), 1, problems);
        if (production.terrainMultipliers() != null) {
            production.terrainMultipliers().forEach((terrain, value) -> {
                if (!knownTerrains.contains(terrain)) {
                    problems.add(at + ": terreno desconhecido em terrainMultipliers: " + terrain);
                }
                positive(at + ": terrainMultipliers." + terrain, value, problems);
            });
        }
    }

    private static void validateExtraction(String at, GameRulesFile.Extraction extraction,
            Set<String> depositResources, List<String> problems) {
        if (extraction.resources() == null || extraction.resources().isEmpty()) {
            problems.add(at + ": extraction.resources precisa ter pelo menos um recurso");
        } else {
            for (String resource : extraction.resources()) {
                if (!depositResources.contains(resource)) {
                    problems.add(at + ": extraction só vale para depósitos " + sorted(depositResources)
                            + ", não " + resource);
                } else if (!MatchRulesProperties.RESOURCES.contains(resource)) {
                    problems.add(at + ": recurso extraído não pode ser estocado: " + resource);
                }
            }
        }
        atLeast(at + ": extraction.perCycle", extraction.perCycle(), 1, problems);
    }

    private static void atLeast(String field, Integer value, int min, List<String> problems) {
        if (value == null || value < min) {
            problems.add(field + " precisa ser no mínimo " + min);
        }
    }

    private static void positive(String field, Double value, List<String> problems) {
        if (value == null || !(value > 0) || value.isInfinite()) {
            problems.add(field + " precisa ser positivo");
        }
    }

    private static List<String> sorted(Set<String> values) {
        return values.stream().sorted().toList();
    }
}
