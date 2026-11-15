package com.warzation.realtime;

import com.warzation.application.rules.GameRules;
import com.warzation.application.rules.GameRulesFile;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Regras de economia e construções em vigor. Os tempos já vêm em segundos e com
 * a velocidade do servidor aplicada, para o cliente não refazer a conta.
 */
@RestController
@RequestMapping("/api/v1/rules")
public class RulesController {

    private final GameRules rules;

    public RulesController(GameRules rules) {
        this.rules = rules;
    }

    public record EconomyView(
            long cycleSeconds,
            int maxCatchUpCycles,
            int foodDemandPerProvince,
            int baseFoodCapacity,
            int cancelRefundPercent,
            Map<String, Double> abundanceMultipliers) {
    }

    public record BuildingView(
            String id,
            String name,
            String era,
            String requiredTechnology,
            Map<String, Integer> cost,
            long buildSeconds,
            GameRulesFile.Production production,
            GameRulesFile.Extraction extraction,
            Integer foodCapacityBonus) {
    }

    public record RulesView(boolean provisional, int speed, EconomyView economy, List<BuildingView> buildings) {
    }

    @GetMapping
    public RulesView rules() {
        GameRulesFile.Economy economy = rules.economy();
        return new RulesView(
                rules.provisional(),
                rules.speed(),
                new EconomyView(rules.cycleDuration().toSeconds(), economy.maxCatchUpCycles(),
                        economy.foodDemandPerProvince(), economy.baseFoodCapacity(),
                        economy.cancelRefundPercent(), economy.abundanceMultipliers()),
                rules.buildings().stream()
                        .map(b -> new BuildingView(b.id(), b.name(), b.era(), b.requiredTechnology(), b.cost(),
                                rules.buildDuration(b).toSeconds(), b.production(), b.extraction(),
                                b.foodCapacityBonus()))
                        .toList());
    }
}
