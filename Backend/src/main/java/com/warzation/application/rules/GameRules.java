package com.warzation.application.rules;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warzation.application.ResourceNotFoundException;
import com.warzation.application.map.ScenarioCatalog;
import com.warzation.application.map.ScenarioFile;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Regras de economia e construções, lidas de Backend/data/rules/game-rules.json
 * e validadas no startup. Dados inválidos impedem a subida.
 *
 * <p>{@code application.rules.speed} acelera todos os tempos (ciclo econômico
 * e obras) para testes manuais; em jogo de verdade vale 1.
 */
@Component
@Lazy(false) // valida no startup mesmo com spring.main.lazy-initialization
public class GameRules implements InitializingBean {

    static final int MAX_SPEED = 3600;
    private static final Logger log = LoggerFactory.getLogger(GameRules.class);

    private final ResourceLoader resources;
    private final ObjectMapper strictMapper;
    private final ScenarioCatalog scenarios;
    private final String location;
    private final int speed;

    private GameRulesFile file;
    private Map<String, GameRulesFile.Building> buildings = Map.of();

    public GameRules(
            ResourceLoader resources,
            ObjectMapper objectMapper,
            ScenarioCatalog scenarios,
            @Value("${application.rules.location:classpath:data/rules/game-rules.json}") String location,
            @Value("${application.rules.speed:1}") int speed) {
        this.resources = resources;
        this.strictMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.scenarios = scenarios;
        this.location = location;
        this.speed = speed;
    }

    @Override
    public void afterPropertiesSet() {
        if (speed < 1 || speed > MAX_SPEED) {
            throw new GameRulesValidationException("application.rules.speed",
                    List.of("precisa estar entre 1 e " + MAX_SPEED + " (atual: " + speed + ")"));
        }
        Resource resource = resources.getResource(location);
        GameRulesFile loaded = read(resource);
        Set<String> terrains = scenarios.all().stream()
                .flatMap(scenario -> scenario.file().terrains().stream())
                .map(ScenarioFile.Terrain::id)
                .collect(Collectors.toSet());
        List<String> problems = GameRulesValidator.validate(loaded, terrains);
        if (!problems.isEmpty()) {
            throw new GameRulesValidationException(location, problems);
        }
        Map<String, GameRulesFile.Building> byId = new LinkedHashMap<>();
        loaded.buildings().forEach(building -> byId.put(building.id(), building));
        file = loaded;
        buildings = Collections.unmodifiableMap(byId);
        log.info("Regras de jogo carregadas: {} construções, ciclo de {}{}{}", buildings.size(),
                cycleDuration(), speed > 1 ? " (velocidade x" + speed + ")" : "",
                Boolean.TRUE.equals(loaded.provisional()) ? ", valores provisórios para teste" : "");
    }

    private GameRulesFile read(Resource resource) {
        if (!resource.exists()) {
            throw new GameRulesValidationException(location, List.of("arquivo não encontrado"));
        }
        try (InputStream input = resource.getInputStream()) {
            return strictMapper.readValue(input, GameRulesFile.class);
        } catch (JsonMappingException exception) {
            String path = "$" + exception.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? "." + ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining());
            throw new GameRulesValidationException(location,
                    List.of(exception.getOriginalMessage().split("\n")[0] + " em " + path));
        } catch (IOException exception) {
            throw new GameRulesValidationException(location, List.of("JSON inválido: " + exception.getMessage()));
        }
    }

    public GameRulesFile.Economy economy() {
        return file.economy();
    }

    public boolean provisional() {
        return Boolean.TRUE.equals(file.provisional());
    }

    public int speed() {
        return speed;
    }

    public Collection<GameRulesFile.Building> buildings() {
        return buildings.values();
    }

    public GameRulesFile.Building building(String id) {
        GameRulesFile.Building building = buildings.get(id);
        if (building == null) {
            throw new ResourceNotFoundException("Construção não encontrada: " + id);
        }
        return building;
    }

    /** Duração real de um ciclo econômico, já com a velocidade aplicada. */
    public Duration cycleDuration() {
        return scaled(file.economy().cycleMinutes());
    }

    /** Duração real de uma obra, já com a velocidade aplicada. */
    public Duration buildDuration(GameRulesFile.Building building) {
        return scaled(building.buildMinutes());
    }

    private Duration scaled(int minutes) {
        long seconds = Math.max(1, Duration.ofMinutes(minutes).toSeconds() / speed);
        return Duration.ofSeconds(seconds);
    }
}
