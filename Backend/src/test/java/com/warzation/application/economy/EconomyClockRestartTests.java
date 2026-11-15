package com.warzation.application.economy;

import static com.warzation.application.economy.EconomyTestSupport.T0;
import static org.assertj.core.api.Assertions.assertThat;

import com.warzation.WarzationApplication;
import com.warzation.application.economy.EconomyTestSupport.AppliedCycle;
import com.warzation.application.economy.EconomyTestSupport.MutableClock;
import com.warzation.application.economy.EconomyTestSupport.RecordingStep;
import com.warzation.application.match.MatchService;
import com.warzation.application.match.MatchView;
import com.warzation.application.player.Player;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Reiniciar o servidor não repete nem pula ciclos: a agenda está no banco. */
class EconomyClockRestartTests {

    private static final Player ONE = new Player("jogador-1", "Jogador 1");
    private static final Player TWO = new Player("jogador-2", "Jogador 2");

    @TempDir
    Path databaseDirectory;

    @Test
    void restartResumesExactlyWhereItStopped() {
        String jdbcUrl = "jdbc:h2:file:" + databaseDirectory.resolve("warzation").toAbsolutePath() + ";MODE=PostgreSQL";
        String matchId;

        try (ConfigurableApplicationContext run = start(jdbcUrl, T0)) {
            MatchService matches = run.getBean(MatchService.class);
            matchId = matches.create(ONE, UUID.randomUUID(), "nordeste-teste", "Reinício").match().id();
            matches.join(ONE, UUID.randomUUID(), matchId, "parnaiba");
            matches.join(TWO, UUID.randomUUID(), matchId, "sao-francisco");
            matches.start(ONE, UUID.randomUUID(), matchId);

            run.getBean(MutableClock.class).set(T0.plus(Duration.ofMinutes(150)));
            assertThat(run.getBean(EconomyClock.class).processDue()).isEqualTo(2);
        }

        // Volta no mesmo instante: nada a repetir.
        try (ConfigurableApplicationContext run = start(jdbcUrl, T0.plus(Duration.ofMinutes(150)))) {
            assertThat(run.getBean(EconomyClock.class).processDue()).isZero();

            run.getBean(MutableClock.class).set(T0.plus(Duration.ofHours(3)));
            assertThat(run.getBean(EconomyClock.class).processDue()).isEqualTo(1);
            assertThat(run.getBean(RecordingStep.class).appliedTo(matchId))
                    .containsExactly(new AppliedCycle(matchId, 3, T0.plus(Duration.ofHours(3))));
        }

        // Volta depois de 5 h desligado: recupera os 5 ciclos perdidos, sem pular.
        try (ConfigurableApplicationContext run = start(jdbcUrl, T0.plus(Duration.ofHours(8)))) {
            assertThat(run.getBean(EconomyClock.class).processDue()).isEqualTo(5);
            assertThat(run.getBean(RecordingStep.class).appliedTo(matchId)).extracting(AppliedCycle::cycle)
                    .containsExactly(4L, 5L, 6L, 7L, 8L);
            MatchView.Economy economy = run.getBean(MatchService.class).get(matchId).economy();
            assertThat(economy.cycles()).isEqualTo(8);
            assertThat(economy.skippedCycles()).isZero();
        }
    }

    private static ConfigurableApplicationContext start(String jdbcUrl, Instant now) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(
                WarzationApplication.class, EconomyTestSupport.class)
                .profiles("test")
                .run("--spring.datasource.url=" + jdbcUrl, "--server.port=0", "--spring.main.banner-mode=off");
        context.getBean(MutableClock.class).set(now);
        return context;
    }
}
