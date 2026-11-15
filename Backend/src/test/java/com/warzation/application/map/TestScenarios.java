package com.warzation.application.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cenário mínimo válido: 4 estados com 2 províncias cada (p1..p8) numa linha
 * p1-p2-...-p8, e duas nações nas pontas. Os testes alteram o mapa devolvido
 * para provocar cada erro.
 */
final class TestScenarios {

    static final ObjectMapper MAPPER = new ObjectMapper();

    private TestScenarios() {
    }

    static Map<String, Object> valid() {
        Map<String, Object> scenario = new LinkedHashMap<>();
        scenario.put("formatVersion", 1);
        scenario.put("id", "teste");
        scenario.put("name", "Cenário de teste");
        scenario.put("provisional", true);
        scenario.put("geometryFile", "provinces.geojson");
        scenario.put("states", new ArrayList<>(List.of(
                Map.of("id", "AA", "name", "A"), Map.of("id", "BB", "name", "B"),
                Map.of("id", "CC", "name", "C"), Map.of("id", "DD", "name", "D"))));
        scenario.put("terrains", new ArrayList<>(List.of(
                Map.of("id", "CERRADO", "name", "Cerrado", "description", "-"))));
        List<Map<String, Object>> provinces = new ArrayList<>();
        String[] states = {"AA", "AA", "BB", "BB", "CC", "CC", "DD", "DD"};
        for (int i = 1; i <= 8; i++) {
            Map<String, Object> province = new LinkedHashMap<>();
            province.put("id", "p" + i);
            province.put("name", "Província " + i);
            province.put("state", states[i - 1]);
            province.put("mainCity", Map.of("name", "Cidade " + i, "lon", i + 0.5, "lat", 0.5));
            province.put("terrain", "CERRADO");
            province.put("coastal", i == 1);
            province.put("deposits", new ArrayList<>(List.of(Map.of("resource", "PEDRA", "abundance", "MEDIA"))));
            provinces.add(province);
        }
        scenario.put("provinces", provinces);
        List<List<String>> connections = new ArrayList<>();
        for (int i = 1; i < 8; i++) {
            connections.add(new ArrayList<>(List.of("p" + i, "p" + (i + 1))));
        }
        scenario.put("connections", connections);
        scenario.put("nations", new ArrayList<>(List.of(
                new LinkedHashMap<>(Map.of("id", "oeste", "name", "Oeste", "capital", "p1",
                        "startingProvinces", new ArrayList<>(List.of("p1", "p2", "p3")))),
                new LinkedHashMap<>(Map.of("id", "leste", "name", "Leste", "capital", "p8",
                        "startingProvinces", new ArrayList<>(List.of("p6", "p7", "p8")))))));
        return scenario;
    }

    static Map<String, Object> geometryFor(int provinces) {
        List<Map<String, Object>> features = new ArrayList<>();
        for (int i = 1; i <= provinces; i++) {
            List<List<Integer>> ring = List.of(List.of(i, 0), List.of(i + 1, 0), List.of(i + 1, 1), List.of(i, 1),
                    List.of(i, 0));
            features.add(Map.of("type", "Feature", "properties", Map.of("id", "p" + i),
                    "geometry", Map.of("type", "Polygon", "coordinates", List.of(ring))));
        }
        return Map.of("type", "FeatureCollection", "features", features);
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> provinces(Map<String, Object> scenario) {
        return (List<Map<String, Object>>) scenario.get("provinces");
    }

    @SuppressWarnings("unchecked")
    static List<List<String>> connections(Map<String, Object> scenario) {
        return (List<List<String>>) scenario.get("connections");
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> nations(Map<String, Object> scenario) {
        return (List<Map<String, Object>>) scenario.get("nations");
    }

    /** Grava o cenário em dir/<id>/ e devolve o caminho do scenario.json. */
    static Path write(Path dir, Map<String, Object> scenario, Map<String, Object> geometry) throws IOException {
        Path folder = Files.createDirectories(dir.resolve(String.valueOf(scenario.get("id"))));
        Files.writeString(folder.resolve("scenario.json"), MAPPER.writeValueAsString(scenario));
        Files.writeString(folder.resolve("provinces.geojson"), MAPPER.writeValueAsString(geometry));
        return folder.resolve("scenario.json");
    }
}
