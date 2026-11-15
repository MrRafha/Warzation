package com.warzation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MatchRulesPropertiesTests {

    @Test
    void appliesDocumentedDefaults() {
        MatchRulesProperties rules = new MatchRulesProperties(null, null, null, null);

        assertThat(rules.minPlayersToStart()).isEqualTo(2);
        assertThat(rules.startingInfantryPerProvince()).isEqualTo(2);
        assertThat(rules.neutralGarrison()).isEqualTo(1);
        assertThat(rules.initialStockPerProvince()).isEmpty();
    }

    @Test
    void rejectsUnknownResourcesAndNegativeValues() {
        assertThatThrownBy(() -> new MatchRulesProperties(2, 2, 1, Map.of("OURO", 10L)))
                .hasMessageContaining("OURO");
        assertThatThrownBy(() -> new MatchRulesProperties(2, 2, 1, Map.of("COMIDA", -1L)))
                .hasMessageContaining("negativo");
        assertThatThrownBy(() -> new MatchRulesProperties(0, 2, 1, Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MatchRulesProperties(2, 0, 1, Map.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
