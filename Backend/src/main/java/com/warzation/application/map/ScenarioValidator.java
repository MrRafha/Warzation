package com.warzation.application.map;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Regras que um cenário precisa cumprir para ser carregado. Espelha
 * {@code tools/mapdata/validate_scenario.py}, menos as checagens geométricas
 * finas (fronteiras reais), que ficam no CI.
 */
public final class ScenarioValidator {

    public static final int SUPPORTED_FORMAT = 1;
    public static final int MIN_PROVINCES = 8;
    public static final int MAX_PROVINCES = 30;
    public static final int MIN_PROVINCES_PER_STATE = 2;
    public static final int MAX_PROVINCES_PER_STATE = 3;
    public static final int STARTING_PROVINCES = 3;

    private static final String ID_PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$";

    private ScenarioValidator() {
    }

    public static void validate(ScenarioFile scenario, JsonNode geometry, String source) {
        List<String> problems = new ArrayList<>();

        if (scenario.formatVersion() != SUPPORTED_FORMAT) {
            problems.add("formatVersion " + scenario.formatVersion() + " não suportada (esperado " + SUPPORTED_FORMAT + ")");
        }
        if (isBlank(scenario.id()) || !scenario.id().matches(ID_PATTERN)) {
            problems.add("id do cenário ausente ou fora do padrão minúsculas-com-hífens");
        }
        if (isBlank(scenario.name())) {
            problems.add("name do cenário ausente");
        }

        Set<String> states = ids(scenario.states(), ScenarioFile.State::id, "estado", problems);
        Set<String> terrains = ids(scenario.terrains(), ScenarioFile.Terrain::id, "terreno", problems);
        Set<String> provinces = ids(scenario.provinces(), ScenarioFile.Province::id, "província", problems);

        if (provinces.size() < MIN_PROVINCES || provinces.size() > MAX_PROVINCES) {
            problems.add("o protótipo precisa de " + MIN_PROVINCES + " a " + MAX_PROVINCES + " províncias; há "
                    + provinces.size());
        }

        Map<String, Integer> perState = new HashMap<>();
        for (ScenarioFile.Province p : nonNull(scenario.provinces())) {
            String where = "província " + p.id();
            if (p.id() != null && !p.id().matches(ID_PATTERN)) {
                problems.add(where + ": id fora do padrão minúsculas-com-hífens");
            }
            if (isBlank(p.name())) {
                problems.add(where + ": name ausente");
            }
            if (!states.contains(p.state())) {
                problems.add(where + ": estado desconhecido " + p.state());
            } else {
                perState.merge(p.state(), 1, Integer::sum);
            }
            if (!terrains.contains(p.terrain())) {
                problems.add(where + ": terreno desconhecido " + p.terrain());
            }
            if (p.coastal() == null) {
                problems.add(where + ": coastal precisa ser true ou false");
            }
            ScenarioFile.MainCity city = p.mainCity();
            if (city == null || isBlank(city.name()) || city.lon() == null || city.lat() == null) {
                problems.add(where + ": cidade principal incompleta");
            }
            Set<ScenarioFile.Resource> seen = new HashSet<>();
            for (ScenarioFile.Deposit deposit : nonNull(p.deposits())) {
                if (deposit.resource() == null || deposit.abundance() == null) {
                    problems.add(where + ": depósito incompleto");
                } else if (!seen.add(deposit.resource())) {
                    problems.add(where + ": depósito de " + deposit.resource() + " repetido");
                }
            }
        }
        for (String state : states) {
            int count = perState.getOrDefault(state, 0);
            if (count < MIN_PROVINCES_PER_STATE || count > MAX_PROVINCES_PER_STATE) {
                problems.add("estado " + state + ": precisa de " + MIN_PROVINCES_PER_STATE + " a "
                        + MAX_PROVINCES_PER_STATE + " províncias; há " + count);
            }
        }

        List<List<String>> validEdges = new ArrayList<>();
        Set<Set<String>> seenEdges = new HashSet<>();
        for (List<String> edge : nonNull(scenario.connections())) {
            if (edge == null || edge.size() != 2 || edge.contains(null)) {
                problems.add("conexão malformada: " + edge);
                continue;
            }
            String a = edge.get(0);
            String b = edge.get(1);
            if (a.equals(b)) {
                problems.add("conexão de " + a + " com ela mesma");
                continue;
            }
            boolean known = true;
            for (String node : edge) {
                if (!provinces.contains(node)) {
                    problems.add("conexão cita província inexistente " + node);
                    known = false;
                }
            }
            if (!known) {
                continue;
            }
            if (!seenEdges.add(Set.of(a, b))) {
                problems.add("conexão duplicada " + a + " - " + b);
                continue;
            }
            validEdges.add(edge);
        }

        ProvinceGraph graph = new ProvinceGraph(provinces, validEdges);
        if (!provinces.isEmpty() && !graph.isConnectedSubset(provinces)) {
            problems.add("o grafo de províncias não é conexo");
        }

        Map<String, String> claimed = new HashMap<>();
        ids(scenario.nations(), ScenarioFile.Nation::id, "nação", problems);
        for (ScenarioFile.Nation nation : nonNull(scenario.nations())) {
            String where = "nação " + nation.id();
            List<String> start = nonNull(nation.startingProvinces());
            if (start.size() != STARTING_PROVINCES || new HashSet<>(start).size() != STARTING_PROVINCES) {
                problems.add(where + ": precisa de exatamente " + STARTING_PROVINCES
                        + " províncias iniciais distintas");
            }
            if (!start.contains(nation.capital())) {
                problems.add(where + ": a capital precisa estar entre as províncias iniciais");
            }
            boolean allKnown = true;
            for (String province : start) {
                if (!provinces.contains(province)) {
                    problems.add(where + ": província inexistente " + province);
                    allKnown = false;
                } else if (claimed.containsKey(province)) {
                    problems.add(where + ": " + province + " já pertence a " + claimed.get(province));
                } else {
                    claimed.put(province, nation.id());
                }
            }
            if (allKnown && !start.isEmpty() && !graph.isConnectedSubset(start)) {
                problems.add(where + ": as províncias iniciais precisam ser contíguas");
            }
        }

        validateGeometry(geometry, provinces, problems);

        if (!problems.isEmpty()) {
            throw new ScenarioValidationException(source, problems);
        }
    }

    private static void validateGeometry(JsonNode geometry, Set<String> provinces, List<String> problems) {
        JsonNode features = geometry == null ? null : geometry.get("features");
        if (features == null || !features.isArray()) {
            problems.add("arquivo de geometria sem 'features'");
            return;
        }
        Set<String> found = new LinkedHashSet<>();
        for (JsonNode feature : features) {
            String id = feature.path("properties").path("id").asText(null);
            String type = feature.path("geometry").path("type").asText("");
            if (id == null) {
                problems.add("geometria sem properties.id");
                continue;
            }
            if (!found.add(id)) {
                problems.add("geometria duplicada para " + id);
            }
            if (!type.equals("Polygon") && !type.equals("MultiPolygon")) {
                problems.add("geometria de " + id + " precisa ser Polygon ou MultiPolygon");
            }
        }
        Set<String> missing = new LinkedHashSet<>(provinces);
        missing.removeAll(found);
        Set<String> extra = new LinkedHashSet<>(found);
        extra.removeAll(provinces);
        if (!missing.isEmpty()) {
            problems.add("províncias sem geometria: " + missing);
        }
        if (!extra.isEmpty()) {
            problems.add("geometrias sem província: " + extra);
        }
    }

    private static <T> Set<String> ids(
            List<T> items, java.util.function.Function<T, String> id, String kind, List<String> problems) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (T item : nonNull(items)) {
            String value = id.apply(item);
            if (isBlank(value)) {
                problems.add(kind + " sem id");
            } else {
                counts.merge(value, 1, Integer::sum);
            }
        }
        counts.forEach((value, count) -> {
            if (count > 1) {
                problems.add(kind + " duplicado: " + value);
            }
        });
        return counts.keySet();
    }

    private static <T> List<T> nonNull(List<T> list) {
        return list == null ? List.of() : list.stream().filter(Objects::nonNull).toList();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
