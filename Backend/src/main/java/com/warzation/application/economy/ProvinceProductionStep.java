package com.warzation.application.economy;

import com.warzation.application.map.LoadedScenario;
import com.warzation.application.map.ScenarioCatalog;
import com.warzation.application.rules.GameRules;
import com.warzation.domain.match.Match;
import com.warzation.domain.match.MatchBuilding;
import com.warzation.domain.match.MatchBuildingRepository;
import com.warzation.domain.match.MatchProvince;
import com.warzation.domain.match.MatchProvinceRepository;
import com.warzation.domain.match.MatchProvinceStock;
import com.warzation.domain.match.MatchProvinceStockRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Aplica, a cada ciclo, a produção das construções em funcionamento e a demanda
 * alimentar fixa no estoque local de cada província com dono.
 *
 * <p>Ordem dentro do ciclo: soma a produção, desconta a demanda e só então
 * limita a comida à capacidade (o excedente é perdido). Se faltar comida, o
 * estoque fica em zero e a falta é registrada na província. Pedra e metal não
 * têm limite e ficam na província. Neutras não participam.
 */
@Component
@Order(ProvinceProductionStep.ORDER)
public class ProvinceProductionStep implements EconomyCycleStep {

    /** Depois da conclusão de obras (#34), para que uma obra pronta no ciclo já produza. */
    public static final int ORDER = 100;

    private final ScenarioCatalog scenarios;
    private final GameRules rules;
    private final MatchProvinceRepository provinces;
    private final MatchProvinceStockRepository stocks;
    private final MatchBuildingRepository buildings;

    public ProvinceProductionStep(ScenarioCatalog scenarios, GameRules rules, MatchProvinceRepository provinces,
            MatchProvinceStockRepository stocks, MatchBuildingRepository buildings) {
        this.scenarios = scenarios;
        this.rules = rules;
        this.provinces = provinces;
        this.stocks = stocks;
        this.buildings = buildings;
    }

    @Override
    public void apply(Match match, long cycle, OffsetDateTime at) {
        LoadedScenario scenario = scenarios.get(match.getScenarioId());
        Map<String, List<String>> operating = buildings
                .findByMatchIdAndStatus(match.getId(), MatchBuilding.Status.COMPLETED).stream()
                .filter(building -> building.operatingAt(at))
                .collect(Collectors.groupingBy(MatchBuilding::getProvinceId,
                        Collectors.mapping(MatchBuilding::getBuildingType, Collectors.toList())));
        Map<String, Map<String, MatchProvinceStock>> stockByProvince = new HashMap<>();
        for (MatchProvinceStock stock : stocks.findByIdMatchId(match.getId())) {
            stockByProvince.computeIfAbsent(stock.getProvinceId(), id -> new HashMap<>())
                    .put(stock.getResource(), stock);
        }
        List<MatchProvinceStock> created = new ArrayList<>();

        for (MatchProvince province : provinces.findByIdMatchId(match.getId())) {
            if (province.getOwnerNationId() == null) {
                continue;
            }
            ProvinceRates rates = ProvinceEconomy.rates(
                    scenario.province(province.getProvinceId()).orElseThrow(),
                    true,
                    operating.getOrDefault(province.getProvinceId(), List.of()),
                    rules::building,
                    rules.economy());
            Map<String, MatchProvinceStock> local =
                    stockByProvince.computeIfAbsent(province.getProvinceId(), id -> new HashMap<>());

            rates.production().forEach((resource, amount) -> {
                if (!ProvinceRates.FOOD.equals(resource)) {
                    MatchProvinceStock stock = stock(match, province, resource, local, created);
                    stock.setAmount(stock.getAmount() + amount);
                }
            });

            MatchProvinceStock food = stock(match, province, ProvinceRates.FOOD, local, created);
            long balance = food.getAmount() + rates.foodBalance();
            long shortage = Math.max(0, -balance);
            food.setAmount(Math.min(Math.max(0, balance), rates.foodCapacity()));
            province.recordFoodShortage(shortage);
        }
        stocks.saveAll(created);
    }

    private static MatchProvinceStock stock(Match match, MatchProvince province, String resource,
            Map<String, MatchProvinceStock> local, List<MatchProvinceStock> created) {
        return local.computeIfAbsent(resource, key -> {
            MatchProvinceStock stock = new MatchProvinceStock(match.getId(), province.getProvinceId(), key, 0);
            created.add(stock);
            return stock;
        });
    }
}
