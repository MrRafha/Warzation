package com.warzation.application.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class ScenarioValidatorTests {

    @Test
    void acceptsTheMinimalScenario() {
        assertThatCode(() -> validate(s -> { }, 8)).doesNotThrowAnyException();
    }

    @Test
    void rejectsDuplicatedProvinceIds() {
        assertThat(problems(s -> TestScenarios.provinces(s).get(1).put("id", "p1"), 8))
                .anyMatch(p -> p.contains("província duplicado: p1"));
    }

    @Test
    void rejectsConnectionsToUnknownProvincesSelfLoopsAndDuplicates() {
        List<String> problems = problems(s -> {
            TestScenarios.connections(s).add(new ArrayList<>(List.of("p1", "p99")));
            TestScenarios.connections(s).add(new ArrayList<>(List.of("p2", "p2")));
            TestScenarios.connections(s).add(new ArrayList<>(List.of("p2", "p1")));
        }, 8);

        assertThat(problems)
                .anyMatch(p -> p.contains("inexistente p99"))
                .anyMatch(p -> p.contains("com ela mesma"))
                .anyMatch(p -> p.contains("duplicada p2 - p1"));
    }

    @Test
    void rejectsDisconnectedGraph() {
        assertThat(problems(s -> TestScenarios.connections(s).remove(3), 8))
                .anyMatch(p -> p.contains("não é conexo"));
    }

    @Test
    void enforcesProvinceCountAndProvincesPerState() {
        assertThat(problems(s -> {
            TestScenarios.provinces(s).remove(7);
            TestScenarios.connections(s).remove(6);
            TestScenarios.nations(s).get(1).put("startingProvinces", new ArrayList<>(List.of("p5", "p6", "p7")));
            TestScenarios.nations(s).get(1).put("capital", "p7");
        }, 7))
                .anyMatch(p -> p.contains("8 a 30 províncias; há 7"))
                .anyMatch(p -> p.contains("estado DD: precisa de 2 a 3 províncias; há 1"));
    }

    @Test
    void rejectsUnknownStateAndTerrain() {
        assertThat(problems(s -> {
            TestScenarios.provinces(s).get(0).put("state", "ZZ");
            TestScenarios.provinces(s).get(0).put("terrain", "VULCAO");
        }, 8))
                .anyMatch(p -> p.contains("estado desconhecido ZZ"))
                .anyMatch(p -> p.contains("terreno desconhecido VULCAO"));
    }

    @Test
    void enforcesNationRules() {
        List<String> problems = problems(s -> {
            Map<String, Object> west = TestScenarios.nations(s).get(0);
            west.put("startingProvinces", new ArrayList<>(List.of("p1", "p2", "p6")));
            west.put("capital", "p5");
        }, 8);

        assertThat(problems)
                .anyMatch(p -> p.contains("capital precisa estar entre as províncias iniciais"))
                .anyMatch(p -> p.contains("p6 já pertence a oeste"))
                .anyMatch(p -> p.contains("nação oeste: as províncias iniciais precisam ser contíguas"));
    }

    @Test
    void requiresExactlyThreeStartingProvinces() {
        assertThat(problems(s -> TestScenarios.nations(s).get(0)
                .put("startingProvinces", new ArrayList<>(List.of("p1", "p2"))), 8))
                .anyMatch(p -> p.contains("exatamente 3 províncias iniciais"));
    }

    @Test
    void requiresOneGeometryPerProvince() {
        assertThat(problems(s -> { }, 7)).anyMatch(p -> p.contains("províncias sem geometria: [p8]"));
        assertThat(problems(s -> { }, 9)).anyMatch(p -> p.contains("geometrias sem província: [p9]"));
    }

    @Test
    void reportsEveryProblemAtOnce() {
        assertThat(problems(s -> {
            TestScenarios.provinces(s).get(0).put("terrain", "X");
            TestScenarios.provinces(s).get(1).put("terrain", "Y");
        }, 8)).hasSizeGreaterThanOrEqualTo(2);
    }

    private static void validate(Consumer<Map<String, Object>> change, int geometries) {
        Map<String, Object> scenario = TestScenarios.valid();
        change.accept(scenario);
        ScenarioFile file = TestScenarios.MAPPER.convertValue(scenario, ScenarioFile.class);
        JsonNode geometry = TestScenarios.MAPPER.valueToTree(TestScenarios.geometryFor(geometries));
        ScenarioValidator.validate(file, geometry, "teste/scenario.json");
    }

    private static List<String> problems(Consumer<Map<String, Object>> change, int geometries) {
        ScenarioValidationException exception = catchThrowableOfType(ScenarioValidationException.class,
                () -> validate(change, geometries));
        assertThat(exception).as("o cenário deveria ser rejeitado").isNotNull();
        assertThat(exception.getSource()).isEqualTo("teste/scenario.json");
        return exception.getProblems();
    }
}
