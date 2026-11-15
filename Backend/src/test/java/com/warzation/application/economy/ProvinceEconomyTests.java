package com.warzation.application.economy;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warzation.application.map.ScenarioFile;
import com.warzation.application.rules.GameRulesFile;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Produção, demanda e capacidade com os valores aprovados e as províncias reais do cenário. */
class ProvinceEconomyTests {

    private static GameRulesFile rules;
    private static Map<String, ScenarioFile.Province> provinces;
    private static Function<String, GameRulesFile.Building> catalog;

    @BeforeAll
    static void load() throws Exception {
        ObjectMapper json = new ObjectMapper();
        rules = json.readValue(Path.of("data/rules/game-rules.json").toFile(), GameRulesFile.class);
        ScenarioFile scenario = json.readValue(Path.of("data/scenarios/nordeste-teste/scenario.json").toFile(),
                ScenarioFile.class);
        provinces = scenario.provinces().stream().collect(Collectors.toMap(ScenarioFile.Province::id, p -> p));
        Map<String, GameRulesFile.Building> byId = rules.buildings().stream()
                .collect(Collectors.toMap(GameRulesFile.Building::id, b -> b));
        catalog = byId::get;
    }

    private static ProvinceRates rates(String province, String... buildings) {
        return ProvinceEconomy.rates(provinces.get(province), true, List.of(buildings), catalog, rules.economy());
    }

    @Test
    void provinceWithoutBuildingsOnlyConsumes() {
        ProvinceRates rates = rates("pi-teresina");
        assertThat(rates.production()).isEmpty();
        assertThat(rates.foodDemand()).isEqualTo(2);
        assertThat(rates.foodCapacity()).isEqualTo(200);
        assertThat(rates.foodBalance()).isEqualTo(-2);
    }

    @Test
    void neutralProvincesHaveNoEconomy() {
        ProvinceRates rates = ProvinceEconomy.rates(provinces.get("pi-teresina"), false, List.of("FAZENDA"),
                catalog, rules.economy());
        assertThat(rates.production()).isEmpty();
        assertThat(rates.foodDemand()).isZero();
    }

    @Test
    void farmOutputDependsOnTerrain() {
        assertThat(rates("pi-parnaiba", "FAZENDA").produced("COMIDA")).isEqualTo(15);   // planície fértil ×1,5
        assertThat(rates("pi-teresina", "FAZENDA").produced("COMIDA")).isEqualTo(10);   // cerrado ×1
        assertThat(rates("ma-imperatriz", "FAZENDA").produced("COMIDA")).isEqualTo(8);  // floresta ×0,75 = 7,5
    }

    @Test
    void corralIsFavouredByPasture() {
        assertThat(rates("ba-vitoria-da-conquista", "CURRAL").produced("COMIDA")).isEqualTo(9);
        assertThat(rates("ba-barreiras", "CURRAL").produced("COMIDA")).isEqualTo(6);
    }

    @Test
    void mineNeedsAStoneDepositAndScalesWithAbundance() {
        assertThat(rates("pi-teresina", "MINA").produced("PEDRA")).isEqualTo(6);   // média
        assertThat(rates("ba-salvador", "MINA").produced("PEDRA")).isEqualTo(3);   // baixa
        assertThat(rates("pi-floriano", "MINA").production()).containsOnlyKeys("PEDRA"); // metal fica para depois
        assertThat(rates("ba-barreiras", "MINA").production()).isEmpty();           // sem depósito
        assertThat(rates("ba-vitoria-da-conquista", "MINA").production()).isEmpty(); // só metal
    }

    @Test
    void granariesAddFoodCapacity() {
        assertThat(rates("pi-parnaiba", "CELEIRO").foodCapacity()).isEqualTo(500);
        assertThat(rates("pi-parnaiba", "CELEIRO", "CELEIRO").foodCapacity()).isEqualTo(800);
    }

    @Test
    void effectsOfSeveralBuildingsAddUp() {
        ProvinceRates rates = rates("pi-parnaiba", "FAZENDA", "FAZENDA", "CELEIRO", "BIBLIOTECA");
        assertThat(rates.produced("COMIDA")).isEqualTo(30);
        assertThat(rates.foodBalance()).isEqualTo(28);
        assertThat(rates.foodCapacity()).isEqualTo(500);
    }
}
