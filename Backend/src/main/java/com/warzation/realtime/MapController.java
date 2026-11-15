package com.warzation.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.warzation.application.map.LoadedScenario;
import com.warzation.application.map.MapQueryService;
import com.warzation.application.map.ScenarioCatalog;
import com.warzation.application.map.ScenarioFile;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Leitura do mapa. O mesmo cenário é devolvido para qualquer cliente. */
@RestController
@RequestMapping("/api/v1/scenarios")
public class MapController {

    private final ScenarioCatalog catalog;
    private final MapQueryService mapQueries;

    public MapController(ScenarioCatalog catalog, MapQueryService mapQueries) {
        this.catalog = catalog;
        this.mapQueries = mapQueries;
    }

    public record ScenarioSummary(String id, String name, boolean provisional, int provinces, int connections) {
    }

    /**
     * Cenário completo: o conteúdo de scenario.json, os espaços de construção
     * por província (regra do jogo) e a geometria em GeoJSON.
     */
    public record ScenarioResponse(ScenarioFile scenario, Map<String, Integer> buildingSlots, JsonNode geometry) {
    }

    public record ProvinceSummary(String id, String name, String state) {
    }

    public record RouteResponse(String from, String to, List<String> provinces, int borders) {
    }

    public record AdjacencyResponse(String from, String to, boolean adjacent) {
    }

    @GetMapping
    public List<ScenarioSummary> listScenarios() {
        return catalog.all().stream()
                .map(s -> new ScenarioSummary(s.id(), s.file().name(), s.file().provisional(),
                        s.file().provinces().size(), s.file().connections().size()))
                .toList();
    }

    @GetMapping("/{scenarioId}")
    public ResponseEntity<ScenarioResponse> getScenario(@PathVariable String scenarioId) {
        LoadedScenario scenario = catalog.get(scenarioId);
        return ResponseEntity.ok()
                .eTag('"' + scenario.contentHash() + '"')
                .cacheControl(CacheControl.noCache())
                .body(new ScenarioResponse(scenario.file(), scenario.buildingSlots(), scenario.geometry()));
    }

    @GetMapping("/{scenarioId}/provinces/{provinceId}/neighbours")
    public List<ProvinceSummary> getNeighbours(@PathVariable String scenarioId, @PathVariable String provinceId) {
        return mapQueries.neighbours(scenarioId, provinceId).stream()
                .map(p -> new ProvinceSummary(p.id(), p.name(), p.state()))
                .toList();
    }

    @GetMapping("/{scenarioId}/adjacency")
    public AdjacencyResponse getAdjacency(
            @PathVariable String scenarioId, @RequestParam String from, @RequestParam String to) {
        return new AdjacencyResponse(from, to, mapQueries.areAdjacent(scenarioId, from, to));
    }

    @GetMapping("/{scenarioId}/routes")
    public RouteResponse getRoute(
            @PathVariable String scenarioId, @RequestParam String from, @RequestParam String to) {
        List<String> provinces = mapQueries.route(scenarioId, from, to);
        return new RouteResponse(from, to, provinces, provinces.size() - 1);
    }
}
