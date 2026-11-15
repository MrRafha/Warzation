package com.warzation.application.map;

import com.warzation.application.ResourceNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Carrega, valida e persiste todos os cenários durante o startup. Se algum
 * cenário for inválido, a aplicação não sobe.
 */
@Component
public class ScenarioCatalog implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ScenarioCatalog.class);

    private final ResourcePatternResolver resources;
    private final ScenarioReader reader;
    private final ScenarioStore store;
    private final String location;
    private Map<String, LoadedScenario> scenarios = Map.of();

    public ScenarioCatalog(
            ResourcePatternResolver resources,
            ScenarioReader reader,
            ScenarioStore store,
            @Value("${application.scenarios.location:classpath*:data/scenarios/*/scenario.json}") String location) {
        this.resources = resources;
        this.reader = reader;
        this.store = store;
        this.location = location;
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        Resource[] found = resources.getResources(location);
        if (found.length == 0) {
            throw new ScenarioValidationException(location, List.of("nenhum cenário encontrado"));
        }
        Map<String, LoadedScenario> loaded = new LinkedHashMap<>();
        for (Resource resource : found) {
            LoadedScenario scenario = reader.read(resource);
            if (loaded.containsKey(scenario.id())) {
                throw new ScenarioValidationException(ScenarioReader.describe(resource),
                        List.of("id de cenário repetido: " + scenario.id()));
            }
            ScenarioStore.SyncResult result = store.sync(scenario);
            log.info("Cenário {} carregado ({} províncias, {} conexões): {}", scenario.id(),
                    scenario.file().provinces().size(), scenario.file().connections().size(), result);
            loaded.put(scenario.id(), scenario);
        }
        scenarios = Collections.unmodifiableMap(loaded);
    }

    public Collection<LoadedScenario> all() {
        return scenarios.values();
    }

    public LoadedScenario get(String id) {
        LoadedScenario scenario = scenarios.get(id);
        if (scenario == null) {
            throw new ResourceNotFoundException("Cenário não encontrado: " + id);
        }
        return scenario;
    }
}
