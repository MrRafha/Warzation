package com.warzation.application.map;

import com.warzation.application.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

/** Consultas de leitura sobre o mapa: vizinhança, adjacência e rotas. */
@Service
public class MapQueryService {

    private final ScenarioCatalog catalog;

    public MapQueryService(ScenarioCatalog catalog) {
        this.catalog = catalog;
    }

    public List<ScenarioFile.Province> neighbours(String scenarioId, String provinceId) {
        LoadedScenario scenario = catalog.get(scenarioId);
        requireProvince(scenario, provinceId);
        var byId = scenario.provincesById();
        return scenario.graph().neighbours(provinceId).stream().map(byId::get).toList();
    }

    public boolean areAdjacent(String scenarioId, String a, String b) {
        LoadedScenario scenario = catalog.get(scenarioId);
        requireProvince(scenario, a);
        requireProvince(scenario, b);
        return scenario.graph().areAdjacent(a, b);
    }

    public List<String> route(String scenarioId, String from, String to) {
        LoadedScenario scenario = catalog.get(scenarioId);
        requireProvince(scenario, from);
        requireProvince(scenario, to);
        return scenario.graph().route(from, to).orElseThrow(() -> new NoRouteException(from, to));
    }

    private static void requireProvince(LoadedScenario scenario, String provinceId) {
        if (!scenario.graph().contains(provinceId)) {
            throw new ResourceNotFoundException("Província não encontrada: " + provinceId);
        }
    }
}
