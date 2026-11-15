package com.warzation.application.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.warzation.application.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MapQueryServiceTests {

    /** Grafo com uma província isolada, impossível num cenário válido, para exercitar a rota inexistente. */
    private final MapQueryService service = new MapQueryService(new FixedCatalog(scenarioWithIsland()));

    @Test
    void routeBetweenUnconnectedProvincesIsAnExplicitError() {
        assertThatThrownBy(() -> service.route("teste", "p1", "ilha"))
                .isInstanceOf(NoRouteException.class)
                .hasMessageContaining("p1").hasMessageContaining("ilha");
    }

    @Test
    void unknownProvinceIsNotFound() {
        assertThatThrownBy(() -> service.neighbours("teste", "p99")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.areAdjacent("teste", "p1", "p99"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void routeAndNeighboursUseTheGraph() {
        assertThat(service.route("teste", "p1", "p3")).containsExactly("p1", "p2", "p3");
        assertThat(service.neighbours("teste", "p2")).extracting(ScenarioFile.Province::id)
                .containsExactlyInAnyOrder("p1", "p3");
    }

    private static LoadedScenario scenarioWithIsland() {
        Map<String, Object> raw = TestScenarios.valid();
        TestScenarios.provinces(raw).add(Map.of("id", "ilha", "name", "Ilha", "state", "AA",
                "mainCity", Map.of("name", "Ilha", "lon", 0.0, "lat", 0.0), "terrain", "CERRADO",
                "coastal", true, "deposits", List.of()));
        ScenarioFile file = TestScenarios.MAPPER.convertValue(raw, ScenarioFile.class);
        JsonNode geometry = TestScenarios.MAPPER.createObjectNode();
        return LoadedScenario.of(file, geometry, "hash");
    }

    private static final class FixedCatalog extends ScenarioCatalog {

        private final LoadedScenario scenario;

        FixedCatalog(LoadedScenario scenario) {
            super(null, null, null, "");
            this.scenario = scenario;
        }

        @Override
        public LoadedScenario get(String id) {
            if (!scenario.id().equals(id)) {
                throw new ResourceNotFoundException("Cenário não encontrado: " + id);
            }
            return scenario;
        }
    }
}
