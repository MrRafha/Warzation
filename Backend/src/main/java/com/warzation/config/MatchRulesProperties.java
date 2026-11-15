package com.warzation.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parâmetros do início da partida. Valores provisórios para teste
 * (Docs/01, seção 14.2): podem mudar no balanceamento sem tocar no código.
 */
@ConfigurationProperties(prefix = "application.match")
public record MatchRulesProperties(
        Integer minPlayersToStart,
        Integer startingInfantryPerProvince,
        Integer neutralGarrison,
        Map<String, Long> initialStockPerProvince) {

    public static final Set<String> RESOURCES = Set.of("COMIDA", "PEDRA", "METAL", "DINHEIRO", "PETROLEO");

    public MatchRulesProperties {
        minPlayersToStart = minPlayersToStart == null ? 2 : minPlayersToStart;
        startingInfantryPerProvince = startingInfantryPerProvince == null ? 2 : startingInfantryPerProvince;
        neutralGarrison = neutralGarrison == null ? 1 : neutralGarrison;
        initialStockPerProvince = initialStockPerProvince == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(initialStockPerProvince));

        if (minPlayersToStart < 1) {
            throw new IllegalArgumentException("application.match.min-players-to-start precisa ser ao menos 1");
        }
        if (startingInfantryPerProvince < 1 || neutralGarrison < 0) {
            throw new IllegalArgumentException("quantidades de unidades iniciais inválidas");
        }
        initialStockPerProvince.forEach((resource, amount) -> {
            if (!RESOURCES.contains(resource)) {
                throw new IllegalArgumentException("recurso desconhecido em initial-stock-per-province: " + resource);
            }
            if (amount == null || amount < 0) {
                throw new IllegalArgumentException("estoque inicial negativo para " + resource);
            }
        });
    }
}
