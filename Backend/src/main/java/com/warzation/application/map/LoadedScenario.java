package com.warzation.application.map;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Cenário validado e pronto para consulta. */
public record LoadedScenario(
        ScenarioFile file,
        JsonNode geometry,
        ProvinceGraph graph,
        Map<String, Integer> buildingSlots,
        Set<String> capitals,
        String contentHash) {

    public static LoadedScenario of(ScenarioFile file, JsonNode geometry, String contentHash) {
        ProvinceGraph graph = new ProvinceGraph(
                file.provinces().stream().map(ScenarioFile.Province::id).toList(), file.connections());
        Set<String> capitals = file.nations().stream().map(ScenarioFile.Nation::capital).collect(Collectors.toSet());
        Map<String, Integer> slots = file.provinces().stream()
                .collect(Collectors.toMap(
                        ScenarioFile.Province::id,
                        province -> BuildingSlots.forProvince(capitals.contains(province.id())),
                        (a, b) -> a,
                        java.util.LinkedHashMap::new));
        return new LoadedScenario(file, geometry, graph, Map.copyOf(slots), Set.copyOf(capitals), contentHash);
    }

    public String id() {
        return file.id();
    }

    public Optional<ScenarioFile.Province> province(String provinceId) {
        return file.provinces().stream().filter(p -> p.id().equals(provinceId)).findFirst();
    }

    public Map<String, ScenarioFile.Province> provincesById() {
        return file.provinces().stream()
                .collect(Collectors.toMap(ScenarioFile.Province::id, Function.identity()));
    }
}
