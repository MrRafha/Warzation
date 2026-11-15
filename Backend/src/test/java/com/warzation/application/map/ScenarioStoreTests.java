package com.warzation.application.map;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Banco próprio: partidas criadas por outros testes impediriam substituir o cenário. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:scenario-store;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class ScenarioStoreTests {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ScenarioStore store;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void startupPersistsTheNordesteScenario() {
        assertThat(count("scenario_provinces")).isEqualTo(24);
        assertThat(count("scenario_connections")).isEqualTo(53);
        assertThat(count("scenario_nations")).isEqualTo(2);
        assertThat(count("scenario_nation_provinces")).isEqualTo(6);
        assertThat(jdbc.queryForObject(
                "SELECT building_slots FROM scenario_provinces WHERE scenario_id = 'nordeste-teste' AND id = ?",
                Integer.class, "ba-salvador")).isEqualTo(4);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM scenario_provinces WHERE scenario_id = 'nordeste-teste' AND building_slots = 3",
                Integer.class)).isEqualTo(22);
    }

    @Test
    void reloadingTheSameScenarioDoesNotDuplicateRows() {
        LoadedScenario scenario = new ScenarioReader(objectMapper)
                .read(new ClassPathResource("data/scenarios/nordeste-teste/scenario.json"));
        Map<String, Integer> before = counts();

        assertThat(store.sync(scenario)).isEqualTo(ScenarioStore.SyncResult.UNCHANGED);
        assertThat(counts()).isEqualTo(before);

        LoadedScenario changed = new LoadedScenario(scenario.file(), scenario.geometry(), scenario.graph(),
                scenario.buildingSlots(), scenario.capitals(), "outro-hash");
        assertThat(store.sync(changed)).isEqualTo(ScenarioStore.SyncResult.REPLACED);
        assertThat(counts()).isEqualTo(before);

        assertThat(store.sync(scenario)).isEqualTo(ScenarioStore.SyncResult.REPLACED);
    }

    private Map<String, Integer> counts() {
        return Map.of(
                "scenarios", count("scenarios"),
                "provinces", count("scenario_provinces"),
                "deposits", count("scenario_province_deposits"),
                "connections", count("scenario_connections"),
                "nations", count("scenario_nations"),
                "nationProvinces", count("scenario_nation_provinces"));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
