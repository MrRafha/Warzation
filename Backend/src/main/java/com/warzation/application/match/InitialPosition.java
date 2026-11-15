package com.warzation.application.match;

import com.warzation.application.map.LoadedScenario;
import com.warzation.application.map.ScenarioFile;
import com.warzation.config.MatchRulesProperties;
import com.warzation.domain.match.MatchProvince;
import com.warzation.domain.match.MatchProvinceStock;
import com.warzation.domain.match.MatchUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Posição inicial (Docs/01, seções 2.4 e 2.5):
 * <ul>
 *   <li>cada nação com jogador recebe suas três províncias iniciais, duas
 *       infantarias básicas em cada e o estoque inicial em cada uma;</li>
 *   <li>todas as demais províncias, inclusive as de nações sem jogador,
 *       começam neutras com uma pequena guarnição e sem estoque;</li>
 *   <li>nenhuma província começa com construções ou estradas.</li>
 * </ul>
 */
public final class InitialPosition {

    public record Result(List<MatchProvince> provinces, List<MatchUnit> units, List<MatchProvinceStock> stocks) {
    }

    private InitialPosition() {
    }

    public static Result build(
            String matchId, LoadedScenario scenario, Set<String> nationsWithPlayers, MatchRulesProperties rules) {
        Map<String, String> ownerByProvince = new HashMap<>();
        for (ScenarioFile.Nation nation : scenario.file().nations()) {
            if (nationsWithPlayers.contains(nation.id())) {
                nation.startingProvinces().forEach(province -> ownerByProvince.put(province, nation.id()));
            }
        }

        List<MatchProvince> provinces = new ArrayList<>();
        List<MatchUnit> units = new ArrayList<>();
        List<MatchProvinceStock> stocks = new ArrayList<>();
        for (ScenarioFile.Province province : scenario.file().provinces()) {
            String owner = ownerByProvince.get(province.id());
            provinces.add(new MatchProvince(matchId, scenario.id(), province.id(), owner));
            if (owner != null) {
                units.add(new MatchUnit(matchId, province.id(), owner, MatchUnit.BASIC_INFANTRY,
                        rules.startingInfantryPerProvince()));
                rules.initialStockPerProvince().forEach((resource, amount) ->
                        stocks.add(new MatchProvinceStock(matchId, province.id(), resource, amount)));
            } else if (rules.neutralGarrison() > 0) {
                units.add(new MatchUnit(matchId, province.id(), null, MatchUnit.BASIC_INFANTRY,
                        rules.neutralGarrison()));
            }
        }
        return new Result(provinces, units, stocks);
    }
}
