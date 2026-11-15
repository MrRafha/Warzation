package com.warzation.application.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.warzation.application.map.LoadedScenario;
import com.warzation.application.map.ScenarioReader;
import com.warzation.config.MatchRulesProperties;
import com.warzation.domain.match.MatchProvince;
import com.warzation.domain.match.MatchProvinceStock;
import com.warzation.domain.match.MatchUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class InitialPositionTests {

    private static final LoadedScenario NORDESTE = new ScenarioReader(new ObjectMapper())
            .read(new ClassPathResource("data/scenarios/nordeste-teste/scenario.json"));
    private static final MatchRulesProperties RULES =
            new MatchRulesProperties(2, 2, 1, Map.of("COMIDA", 60L, "PEDRA", 20L));

    @Test
    void eachNationStartsWithThreeProvincesAndSixInfantry() {
        InitialPosition.Result result = InitialPosition.build("m", NORDESTE, Set.of("parnaiba", "sao-francisco"), RULES);

        assertThat(result.provinces()).hasSize(24);
        for (String nation : Set.of("parnaiba", "sao-francisco")) {
            assertThat(result.provinces()).filteredOn(p -> nation.equals(p.getOwnerNationId())).hasSize(3);
            assertThat(result.units().stream().filter(u -> nation.equals(u.getOwnerNationId()))
                    .mapToInt(MatchUnit::getQuantity).sum()).isEqualTo(6);
        }
        assertThat(ownerOf(result, "pi-teresina")).isEqualTo("parnaiba");
        assertThat(ownerOf(result, "ba-salvador")).isEqualTo("sao-francisco");
        assertThat(result.units()).filteredOn(u -> u.getOwnerNationId() != null)
                .allSatisfy(u -> {
                    assertThat(u.getQuantity()).isEqualTo(2);
                    assertThat(u.getUnitType()).isEqualTo(MatchUnit.BASIC_INFANTRY);
                });
    }

    @Test
    void forceAndStocksAreEqualBetweenNations() {
        InitialPosition.Result result = InitialPosition.build("m", NORDESTE, Set.of("parnaiba", "sao-francisco"), RULES);
        Map<String, String> owner = result.provinces().stream()
                .filter(p -> p.getOwnerNationId() != null)
                .collect(Collectors.toMap(MatchProvince::getProvinceId, MatchProvince::getOwnerNationId));

        Map<String, Long> stockByNation = result.stocks().stream()
                .collect(Collectors.groupingBy(s -> owner.get(s.getProvinceId()),
                        Collectors.summingLong(MatchProvinceStock::getAmount)));
        assertThat(stockByNation).containsOnlyKeys("parnaiba", "sao-francisco");
        assertThat(stockByNation.get("parnaiba")).isEqualTo(stockByNation.get("sao-francisco")).isEqualTo(240L);
        assertThat(result.stocks()).allSatisfy(s -> assertThat(owner).containsKey(s.getProvinceId()));
    }

    @Test
    void otherProvincesAreNeutralWithASmallGarrisonAndNoStock() {
        InitialPosition.Result result = InitialPosition.build("m", NORDESTE, Set.of("parnaiba", "sao-francisco"), RULES);

        assertThat(result.provinces()).filteredOn(p -> p.getOwnerNationId() == null).hasSize(18);
        assertThat(result.units()).filteredOn(u -> u.getOwnerNationId() == null)
                .hasSize(18)
                .allSatisfy(u -> assertThat(u.getQuantity()).isEqualTo(1));
    }

    @Test
    void nationWithoutPlayerStaysNeutral() {
        InitialPosition.Result result = InitialPosition.build("m", NORDESTE, Set.of("parnaiba"), RULES);

        assertThat(ownerOf(result, "ba-salvador")).isNull();
        assertThat(result.provinces()).filteredOn(p -> p.getOwnerNationId() != null).hasSize(3);
        assertThat(result.provinces()).filteredOn(p -> p.getOwnerNationId() == null).hasSize(21);
    }

    @Test
    void neutralGarrisonCanBeDisabled() {
        InitialPosition.Result result = InitialPosition.build("m", NORDESTE, Set.of("parnaiba", "sao-francisco"),
                new MatchRulesProperties(2, 2, 0, Map.of()));

        assertThat(result.units()).hasSize(6).allSatisfy(u -> assertThat(u.getOwnerNationId()).isNotNull());
        assertThat(result.stocks()).isEmpty();
    }

    private static String ownerOf(InitialPosition.Result result, String province) {
        return result.provinces().stream()
                .filter(p -> p.getProvinceId().equals(province))
                .map(MatchProvince::getOwnerNationId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
