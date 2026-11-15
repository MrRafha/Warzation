package com.warzation.application.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.warzation.application.map.LoadedScenario;
import com.warzation.application.map.ScenarioCatalog;
import com.warzation.application.map.ScenarioStore;
import com.warzation.application.map.ScenarioValidationException;
import com.warzation.application.player.Player;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Um cenário usado por partidas não pode ter o conteúdo trocado: as partidas dependem dele. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:scenario-in-use;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class ScenarioInUseTests {

    @Autowired
    private MatchService matches;

    @Autowired
    private ScenarioCatalog catalog;

    @Autowired
    private ScenarioStore store;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void changedScenarioUsedByMatchesIsRefused() {
        matches.create(new Player("jogador-1", "Jogador 1"), UUID.randomUUID(), "nordeste-teste", null);
        LoadedScenario current = catalog.get("nordeste-teste");
        LoadedScenario changed = new LoadedScenario(current.file(), current.geometry(), current.graph(),
                current.buildingSlots(), current.capitals(), "conteudo-alterado");

        assertThat(store.sync(current)).isEqualTo(ScenarioStore.SyncResult.UNCHANGED);
        assertThatThrownBy(() -> store.sync(changed))
                .isInstanceOf(ScenarioValidationException.class)
                .hasMessageContaining("novo id de cenário");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM scenario_provinces", Integer.class)).isEqualTo(24);
    }
}
