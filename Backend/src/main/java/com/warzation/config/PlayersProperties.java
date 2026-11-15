package com.warzation.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Jogadores de teste do protótipo. Solução provisória: não há contas, só
 * tokens configurados localmente (variáveis de ambiente ou application-local.yml).
 * Jogador com token vazio fica desativado.
 */
@ConfigurationProperties(prefix = "application.players")
public record PlayersProperties(List<TestPlayer> testPlayers) {

    public PlayersProperties {
        testPlayers = testPlayers == null ? List.of() : List.copyOf(testPlayers);
    }

    public record TestPlayer(String id, String name, String token) {
    }
}
