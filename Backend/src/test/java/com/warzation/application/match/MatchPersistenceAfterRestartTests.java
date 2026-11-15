package com.warzation.application.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.warzation.WarzationApplication;
import com.warzation.application.player.Player;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** A partida, os participantes e a proteção contra reenvio sobrevivem ao reinício do servidor. */
class MatchPersistenceAfterRestartTests {

    private static final Player ONE = new Player("jogador-1", "Jogador 1");
    private static final Player TWO = new Player("jogador-2", "Jogador 2");

    @TempDir
    Path databaseDirectory;

    @Test
    void matchStateSurvivesRestart() {
        String jdbcUrl = "jdbc:h2:file:" + databaseDirectory.resolve("warzation").toAbsolutePath() + ";MODE=PostgreSQL";
        UUID joinCommand = UUID.randomUUID();
        String matchId;

        try (ConfigurableApplicationContext firstRun = start(jdbcUrl)) {
            MatchService matches = firstRun.getBean(MatchService.class);
            matchId = matches.create(ONE, UUID.randomUUID(), "nordeste-teste", "Persistente").match().id();
            matches.join(ONE, joinCommand, matchId, "parnaiba");
            matches.join(TWO, UUID.randomUUID(), matchId, "sao-francisco");
        }

        try (ConfigurableApplicationContext secondRun = start(jdbcUrl)) {
            MatchService matches = secondRun.getBean(MatchService.class);
            MatchView recovered = matches.get(matchId);

            assertThat(recovered.name()).isEqualTo("Persistente");
            assertThat(recovered.status()).isEqualTo("OPEN");
            assertThat(recovered.participants()).extracting(MatchView.Participant::nationId)
                    .containsExactly("parnaiba", "sao-francisco");

            MatchCommandResult resent = matches.join(ONE, joinCommand, matchId, "parnaiba");
            assertThat(resent.status()).isEqualTo(MatchCommandResult.Status.DUPLICATE);
            assertThat(matches.get(matchId).participants()).hasSize(2);
        }
    }

    private static ConfigurableApplicationContext start(String jdbcUrl) {
        return new SpringApplicationBuilder(WarzationApplication.class)
                .profiles("test")
                .run("--spring.datasource.url=" + jdbcUrl, "--server.port=0", "--spring.main.banner-mode=off");
    }
}
