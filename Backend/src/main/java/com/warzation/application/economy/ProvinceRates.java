package com.warzation.application.economy;

import java.util.Map;

/**
 * Economia de uma província por ciclo: o que produz, quanto de comida consome e
 * quanto de comida consegue guardar.
 */
public record ProvinceRates(Map<String, Long> production, long foodDemand, long foodCapacity) {

    public static final String FOOD = "COMIDA";

    public long produced(String resource) {
        return production.getOrDefault(resource, 0L);
    }

    /** Saldo de comida por ciclo (pode ser negativo). */
    public long foodBalance() {
        return produced(FOOD) - foodDemand;
    }
}
