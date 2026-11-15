package com.warzation.application.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Cada tipo de dado inválido nas regras é apontado pelo validador. */
class GameRulesValidatorTests {

    private static final Set<String> TERRAINS = Set.of("PLANICIE_FERTIL", "PASTAGEM", "CERRADO", "CAATINGA",
            "FLORESTA");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void theShippedRulesAreValid() throws Exception {
        GameRulesFile file = JSON.readValue(Path.of("data/rules/game-rules.json").toFile(), GameRulesFile.class);
        assertThat(GameRulesValidator.validate(file, TERRAINS)).isEmpty();
        assertThat(file.buildings()).extracting(GameRulesFile.Building::id)
                .containsExactly("FAZENDA", "CURRAL", "CELEIRO", "MINA", "ACAMPAMENTO", "BIBLIOTECA");
    }

    @Test
    void rejectsBuildTimesOver24Hours() throws Exception {
        assertThat(problemsAfter(rules -> building(rules, 0).put("buildMinutes", 24 * 60 + 1)))
                .anyMatch(p -> p.contains("FAZENDA") && p.contains("buildMinutes") && p.contains("24 h"));
    }

    @Test
    void rejectsUnknownResourceInCost() throws Exception {
        assertThat(problemsAfter(rules -> building(rules, 0).put("cost", Map.of("OURO", 10))))
                .anyMatch(p -> p.contains("recurso desconhecido no custo: OURO"));
    }

    @Test
    void rejectsNonPositiveCost() throws Exception {
        assertThat(problemsAfter(rules -> building(rules, 0).put("cost", Map.of("PEDRA", 0))))
                .anyMatch(p -> p.contains("custo de PEDRA precisa ser positivo"));
    }

    @Test
    void rejectsUnknownTerrain() throws Exception {
        assertThat(problemsAfter(rules -> production(rules, 0).put("terrainMultipliers", Map.of("VULCAO", 2.0))))
                .anyMatch(p -> p.contains("terreno desconhecido em terrainMultipliers: VULCAO"));
    }

    @Test
    void rejectsExtractionOfNonDepositResource() throws Exception {
        assertThat(problemsAfter(rules -> extraction(rules, 3).put("resources", List.of("COMIDA"))))
                .anyMatch(p -> p.contains("extraction só vale para depósitos"));
    }

    @Test
    void rejectsDuplicatedBuildingIds() throws Exception {
        assertThat(problemsAfter(rules -> building(rules, 1).put("id", "FAZENDA")))
                .contains("id de construção repetido: FAZENDA");
    }

    @Test
    void rejectsUnknownEra() throws Exception {
        assertThat(problemsAfter(rules -> building(rules, 0).put("era", "PEDRA_LASCADA")))
                .anyMatch(p -> p.contains("era desconhecida"));
    }

    @Test
    void rejectsMissingRequiredNumbersInsteadOfTreatingThemAsZero() throws Exception {
        assertThat(problemsAfter(rules -> economy(rules).remove("cycleMinutes")))
                .contains("economy.cycleMinutes precisa ser no mínimo 1");
    }

    @Test
    void rejectsRefundOutsidePercentRange() throws Exception {
        assertThat(problemsAfter(rules -> economy(rules).put("cancelRefundPercent", 150)))
                .anyMatch(p -> p.contains("cancelRefundPercent"));
    }

    @Test
    void requiresEveryAbundanceMultiplier() throws Exception {
        assertThat(problemsAfter(rules -> economy(rules).put("abundanceMultipliers", Map.of("ALTA", 1.5))))
                .anyMatch(p -> p.contains("abundanceMultipliers precisa ter exatamente [ALTA, BAIXA, MEDIA]"));
    }

    @Test
    void reportsEveryProblemAtOnce() throws Exception {
        List<String> problems = problemsAfter(rules -> {
            building(rules, 0).put("buildMinutes", 0);
            building(rules, 1).put("cost", Map.of());
        });
        assertThat(problems).hasSizeGreaterThanOrEqualTo(2);
    }

    // --- helpers -------------------------------------------------------------

    private static List<String> problemsAfter(Consumer<Map<String, Object>> change) throws Exception {
        Map<String, Object> rules = JSON.readValue(Path.of("data/rules/game-rules.json").toFile(),
                new TypeReference<>() { });
        change.accept(rules);
        return GameRulesValidator.validate(JSON.convertValue(rules, GameRulesFile.class), TERRAINS);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> economy(Map<String, Object> rules) {
        return (Map<String, Object>) rules.get("economy");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> building(Map<String, Object> rules, int index) {
        return ((List<Map<String, Object>>) rules.get("buildings")).get(index);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> production(Map<String, Object> rules, int index) {
        return (Map<String, Object>) building(rules, index).get("production");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extraction(Map<String, Object> rules, int index) {
        return (Map<String, Object>) building(rules, index).get("extraction");
    }
}
