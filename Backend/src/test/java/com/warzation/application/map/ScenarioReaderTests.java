package com.warzation.application.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

class ScenarioReaderTests {

    private final ScenarioReader reader = new ScenarioReader(new ObjectMapper());

    @TempDir
    Path dir;

    @Test
    void readsTheBundledNordesteScenario() {
        LoadedScenario scenario = reader.read(new ClassPathResource("data/scenarios/nordeste-teste/scenario.json"));

        assertThat(scenario.id()).isEqualTo("nordeste-teste");
        assertThat(scenario.file().provinces()).hasSize(24);
        assertThat(scenario.graph().provinces()).hasSize(24);
        assertThat(scenario.geometry().get("features")).hasSize(24);
        assertThat(scenario.capitals()).containsExactlyInAnyOrder("pi-teresina", "ba-salvador");
        assertThat(scenario.buildingSlots()).containsEntry("pi-teresina", 4).containsEntry("pe-recife", 3);
        assertThat(scenario.buildingSlots().values()).allMatch(slots -> slots == 3 || slots == 4);
        assertThat(scenario.contentHash()).hasSize(64);
    }

    @Test
    void readsAValidMinimalScenario() throws Exception {
        LoadedScenario scenario = read(s -> { });

        assertThat(scenario.graph().route("p1", "p8")).hasValueSatisfying(route -> assertThat(route).hasSize(8));
        assertThat(scenario.buildingSlots()).containsEntry("p1", 4).containsEntry("p2", 3).containsEntry("p8", 4);
    }

    @Test
    void sameContentProducesTheSameHash() throws Exception {
        String first = read(s -> { }).contentHash();
        String second = read(s -> { }).contentHash();
        String changed = read(s -> s.put("name", "Outro nome")).contentHash();

        assertThat(first).isEqualTo(second).isNotEqualTo(changed);
    }

    @Test
    void rejectsBuildingsOrRoadsInTheScenario() {
        assertProblem(s -> TestScenarios.provinces(s).get(0).put("buildings", List.of("FAZENDA")), "buildings");
        assertProblem(s -> TestScenarios.provinces(s).get(0).put("roads", List.of()), "roads");
    }

    @Test
    void rejectsUnknownEnumValues() {
        assertProblem(s -> TestScenarios.provinces(s).get(0)
                .put("deposits", List.of(Map.of("resource", "OURO", "abundance", "ALTA"))), "OURO");
    }

    @Test
    void rejectsMissingGeometryFile() {
        assertProblem(s -> s.put("geometryFile", "nao-existe.geojson"), "geometria não encontrado");
        assertProblem(s -> s.put("geometryFile", "../fora.geojson"), "mesma pasta");
    }

    @Test
    void problemsPointToTheFile() throws Exception {
        Map<String, Object> scenario = TestScenarios.valid();
        TestScenarios.connections(scenario).add(new ArrayList<>(List.of("p1", "p9")));
        Path file = TestScenarios.write(dir, scenario, TestScenarios.geometryFor(8));

        assertThatThrownBy(() -> reader.read(new FileSystemResource(file)))
                .isInstanceOfSatisfying(ScenarioValidationException.class, exception -> {
                    assertThat(exception.getSource()).endsWith("scenario.json");
                    assertThat(exception.getProblems()).anyMatch(p -> p.contains("p9"));
                });
    }

    @Test
    void rejectsInvalidJson() throws Exception {
        Path folder = Files.createDirectories(dir.resolve("quebrado"));
        Files.writeString(folder.resolve("scenario.json"), "{ \"formatVersion\": 1, ");

        assertThatThrownBy(() -> reader.read(new FileSystemResource(folder.resolve("scenario.json"))))
                .isInstanceOf(ScenarioValidationException.class)
                .hasMessageContaining("JSON inválido");
    }

    private LoadedScenario read(Consumer<Map<String, Object>> change) throws Exception {
        Map<String, Object> scenario = TestScenarios.valid();
        change.accept(scenario);
        Path base = Files.createTempDirectory(dir, "s");
        return reader.read(new FileSystemResource(TestScenarios.write(base, scenario, TestScenarios.geometryFor(8))));
    }

    private void assertProblem(Consumer<Map<String, Object>> change, String expected) {
        assertThatThrownBy(() -> read(change))
                .isInstanceOf(ScenarioValidationException.class)
                .hasMessageContaining(expected);
    }
}
