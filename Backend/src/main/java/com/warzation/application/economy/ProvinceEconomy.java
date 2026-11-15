package com.warzation.application.economy;

import com.warzation.application.map.ScenarioFile;
import com.warzation.application.rules.GameRulesFile;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Calcula a economia de uma província a partir do terreno, dos depósitos e das
 * construções em funcionamento. Valores fracionários são arredondados para o
 * inteiro mais próximo (7,5 vira 8), para que o saldo seja sempre inteiro e
 * reproduzível.
 */
public final class ProvinceEconomy {

    private ProvinceEconomy() {
    }

    /**
     * @param owned províncias neutras não participam da economia
     * @param buildingTypes ids das construções em funcionamento
     */
    public static ProvinceRates rates(
            ScenarioFile.Province province,
            boolean owned,
            Collection<String> buildingTypes,
            Function<String, GameRulesFile.Building> catalog,
            GameRulesFile.Economy economy) {
        if (!owned) {
            return new ProvinceRates(Map.of(), 0, 0);
        }
        Map<String, Long> production = new TreeMap<>();
        long capacity = economy.baseFoodCapacity();
        for (String type : buildingTypes) {
            GameRulesFile.Building building = catalog.apply(type);
            if (building.production() != null) {
                GameRulesFile.Production rule = building.production();
                double terrain = rule.terrainMultipliers() == null ? 1.0
                        : rule.terrainMultipliers().getOrDefault(province.terrain(), 1.0);
                production.merge(rule.resource(), Math.round(rule.perCycle() * terrain), Long::sum);
            }
            if (building.extraction() != null) {
                extract(province, building.extraction(), economy, production);
            }
            if (building.foodCapacityBonus() != null) {
                capacity += building.foodCapacityBonus();
            }
        }
        production.values().removeIf(amount -> amount == 0);
        return new ProvinceRates(Map.copyOf(production), economy.foodDemandPerProvince(), capacity);
    }

    /** Extrai o primeiro recurso da regra que tenha depósito na província; sem depósito, nada. */
    private static void extract(ScenarioFile.Province province, GameRulesFile.Extraction rule,
            GameRulesFile.Economy economy, Map<String, Long> production) {
        for (String resource : rule.resources()) {
            for (ScenarioFile.Deposit deposit : province.deposits()) {
                if (deposit.resource().name().equals(resource)) {
                    double abundance = economy.abundanceMultipliers().get(deposit.abundance().name());
                    production.merge(resource, Math.round(rule.perCycle() * abundance), Long::sum);
                    return;
                }
            }
        }
    }
}
