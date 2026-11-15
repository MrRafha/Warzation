package com.warzation.application.economy;

import static com.warzation.application.economy.EconomyTestSupport.T0;
import static org.assertj.core.api.Assertions.assertThat;

import com.warzation.application.economy.EconomyTestSupport.MutableClock;
import com.warzation.application.match.MatchService;
import com.warzation.application.match.MatchStateView;
import com.warzation.application.player.Player;
import com.warzation.domain.match.MatchBuilding;
import com.warzation.domain.match.MatchBuildingRepository;
import com.warzation.domain.match.MatchProvinceStockId;
import com.warzation.domain.match.MatchProvinceStockRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Produção, demanda e armazenamento aplicados pelo relógio econômico, com jogadores offline. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:province-production;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@Import(EconomyTestSupport.class)
class ProvinceProductionTests {

    private static final Player ONE = new Player("jogador-1", "Jogador 1");
    private static final Player TWO = new Player("jogador-2", "Jogador 2");

    @Autowired
    private MatchService matchService;

    @Autowired
    private EconomyClock economy;

    @Autowired
    private MutableClock clock;

    @Autowired
    private MatchBuildingRepository buildings;

    @Autowired
    private MatchProvinceStockRepository stocks;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String matchId;

    @BeforeEach
    void startMatch() {
        clock.set(T0);
        matchId = matchService.create(ONE, UUID.randomUUID(), "nordeste-teste", "Produção").match().id();
        matchService.join(ONE, UUID.randomUUID(), matchId, "parnaiba");
        matchService.join(TWO, UUID.randomUUID(), matchId, "sao-francisco");
        matchService.start(ONE, UUID.randomUUID(), matchId);
    }

    @Test
    void provincesWithoutBuildingsOnlyConsumeFood() {
        Map<String, Long> neutralBefore = stock("ma-sao-luis");

        runCycles(3);

        for (String id : new String[] {"pi-teresina", "pi-parnaiba", "ba-barreiras"}) {
            assertThat(stock(id)).containsEntry("COMIDA", 54L).containsEntry("PEDRA", 20L);
        }
        assertThat(stock("ma-sao-luis")).isEqualTo(neutralBefore);
        assertThat(province("ma-sao-luis").economy()).isNull();
    }

    @Test
    void balanceAfterManyOfflineCyclesMatchesTheExpectedValues() {
        build("pi-parnaiba", "FAZENDA");          // +15 -2 = +13 comida
        build("pi-teresina", "MINA");             // +6 pedra (depósito médio)
        build("pi-floriano", "MINA");             // +6 pedra (o metal fica para a Era Medieval)
        build("ba-salvador", "MINA");             // +3 pedra (depósito baixo)
        build("ba-vitoria-da-conquista", "CURRAL"); // +9 -2 = +7 comida (pastagem)
        build("ba-barreiras", "FAZENDA");         // +10 -2 = +8 comida (cerrado)

        runCycles(10);

        assertThat(stock("pi-parnaiba")).containsEntry("COMIDA", 190L);
        assertThat(stock("pi-teresina")).containsEntry("COMIDA", 40L).containsEntry("PEDRA", 80L);
        assertThat(stock("pi-floriano")).containsEntry("COMIDA", 40L).containsEntry("PEDRA", 80L)
                .doesNotContainKey("METAL");
        assertThat(stock("ba-salvador")).containsEntry("PEDRA", 50L);
        assertThat(stock("ba-vitoria-da-conquista")).containsEntry("COMIDA", 130L);
        assertThat(stock("ba-barreiras")).containsEntry("COMIDA", 140L);
    }

    @Test
    void foodAboveCapacityIsLost() {
        build("pi-parnaiba", "FAZENDA");

        runCycles(12); // 60 + 12 × 13 = 216, limitado a 200

        assertThat(stock("pi-parnaiba")).containsEntry("COMIDA", 200L);
    }

    @Test
    void granaryRaisesTheLocalFoodCapacity() {
        build("pi-parnaiba", "FAZENDA");
        build("pi-parnaiba", "CELEIRO");

        runCycles(12);

        assertThat(stock("pi-parnaiba")).containsEntry("COMIDA", 216L);
        assertThat(province("pi-parnaiba").economy().foodCapacity()).isEqualTo(500);
    }

    @Test
    void stockNeverGoesNegativeAndTheShortageIsRecorded() {
        setStock("ba-barreiras", "COMIDA", 3);

        runCycles(3); // 3 → 1 → 0 (faltou 1) → 0 (faltaram 2)

        assertThat(stock("ba-barreiras")).containsEntry("COMIDA", 0L);
        MatchStateView.Economy starving = province("ba-barreiras").economy();
        assertThat(starving.inFoodDeficit()).isTrue();
        assertThat(starving.foodShortageLastCycle()).isEqualTo(2);
        assertThat(starving.foodShortageTotal()).isEqualTo(3);

        build("ba-barreiras", "FAZENDA");
        runCycles(1);

        assertThat(stock("ba-barreiras")).containsEntry("COMIDA", 8L);
        MatchStateView.Economy fed = province("ba-barreiras").economy();
        assertThat(fed.inFoodDeficit()).isFalse();
        assertThat(fed.foodShortageLastCycle()).isZero();
        assertThat(fed.foodShortageTotal()).isEqualTo(3);
    }

    @Test
    void aBuildingOnlyProducesInCyclesAfterItIsReady() {
        buildings.save(MatchBuilding.completed(matchId, "pi-parnaiba", "FAZENDA", at(Duration.ofMinutes(90))));

        clock.set(T0.plus(Duration.ofHours(2)));
        assertThat(economy.processMatch(matchId, clock.offsetNow())).isEqualTo(2);

        // Ciclo 1 (1 h): ainda em obra, só consome. Ciclo 2 (2 h): produz.
        assertThat(stock("pi-parnaiba")).containsEntry("COMIDA", 60L - 2 + 13);
    }

    @Test
    void underConstructionBuildingsDoNotProduce() {
        buildings.save(MatchBuilding.underConstruction(matchId, "pi-parnaiba", "FAZENDA", at(Duration.ZERO),
                at(Duration.ofHours(10))));

        runCycles(2);

        assertThat(stock("pi-parnaiba")).containsEntry("COMIDA", 56L);
    }

    @Test
    void stateShowsRatesBuildingsAndDeficit() {
        build("pi-parnaiba", "FAZENDA");

        MatchStateView.Province parnaiba = province("pi-parnaiba");
        assertThat(parnaiba.buildings()).singleElement()
                .satisfies(b -> {
                    assertThat(b.type()).isEqualTo("FAZENDA");
                    assertThat(b.status()).isEqualTo("COMPLETED");
                });
        assertThat(parnaiba.economy().production()).containsExactly(Map.entry("COMIDA", 15L));
        assertThat(parnaiba.economy().foodDemand()).isEqualTo(2);
        assertThat(parnaiba.economy().foodBalance()).isEqualTo(13);
        assertThat(parnaiba.economy().foodCapacity()).isEqualTo(200);
        assertThat(parnaiba.economy().inFoodDeficit()).isFalse();

        assertThat(province("pi-teresina").economy().foodBalance()).isEqualTo(-2);
        assertThat(province("ma-sao-luis").buildings()).isEmpty();
    }

    // --- helpers -------------------------------------------------------------

    private void runCycles(int count) {
        for (int i = 0; i < count; i++) {
            clock.advance(Duration.ofHours(1));
            assertThat(economy.processMatch(matchId, clock.offsetNow())).isEqualTo(1);
        }
    }

    private void build(String provinceId, String type) {
        buildings.save(MatchBuilding.completed(matchId, provinceId, type, clock.offsetNow()));
    }

    private void setStock(String provinceId, String resource, long amount) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                stocks.findById(new MatchProvinceStockId(matchId, provinceId, resource)).orElseThrow()
                        .setAmount(amount));
    }

    private MatchStateView.Province province(String provinceId) {
        return matchService.state(ONE, matchId).provinces().stream()
                .filter(p -> p.provinceId().equals(provinceId))
                .findFirst().orElseThrow();
    }

    private Map<String, Long> stock(String provinceId) {
        return province(provinceId).stock();
    }

    private static OffsetDateTime at(Duration sinceStart) {
        return OffsetDateTime.ofInstant(T0.plus(sinceStart), ZoneOffset.UTC);
    }
}
