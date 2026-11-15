package com.warzation.application.economy;

import static com.warzation.application.economy.EconomyTestSupport.T0;
import static org.assertj.core.api.Assertions.assertThat;

import com.warzation.application.economy.EconomyTestSupport.AppliedCycle;
import com.warzation.application.economy.EconomyTestSupport.MutableClock;
import com.warzation.application.economy.EconomyTestSupport.RecordingStep;
import com.warzation.application.match.MatchService;
import com.warzation.application.match.MatchView;
import com.warzation.application.player.Player;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** O relógio econômico aplica cada ciclo uma única vez, sem depender de clientes conectados. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:economy-clock;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@ActiveProfiles("test")
@Import(EconomyTestSupport.class)
class EconomyClockTests {

    private static final Player ONE = new Player("jogador-1", "Jogador 1");
    private static final Player TWO = new Player("jogador-2", "Jogador 2");

    @Autowired
    private MatchService matchService;

    @Autowired
    private EconomyClock economy;

    @Autowired
    private MutableClock clock;

    @Autowired
    private RecordingStep recorded;

    @BeforeEach
    void resetClock() {
        clock.set(T0);
    }

    @Test
    void cyclesRunWithNoClientConnected() {
        String matchId = startedMatch();

        clock.advance(Duration.ofMinutes(59));
        economy.processDue();
        assertThat(recorded.appliedTo(matchId)).isEmpty();

        clock.advance(Duration.ofMinutes(1));
        economy.processDue();
        assertThat(recorded.appliedTo(matchId)).containsExactly(
                new AppliedCycle(matchId, 1, T0.plus(Duration.ofHours(1))));

        clock.advance(Duration.ofHours(2).plusMinutes(10));
        economy.processDue();
        assertThat(recorded.appliedTo(matchId)).extracting(AppliedCycle::cycle).containsExactly(1L, 2L, 3L);
        assertThat(recorded.appliedTo(matchId)).extracting(AppliedCycle::at).containsExactly(
                T0.plus(Duration.ofHours(1)), T0.plus(Duration.ofHours(2)), T0.plus(Duration.ofHours(3)));

        MatchView.Economy view = matchService.get(matchId).economy();
        assertThat(view.cycles()).isEqualTo(3);
        assertThat(view.skippedCycles()).isZero();
        assertThat(view.nextCycleAt().toInstant()).isEqualTo(T0.plus(Duration.ofHours(4)));
    }

    @Test
    void processingAgainAtTheSameMomentAppliesNothing() {
        String matchId = startedMatch();
        clock.advance(Duration.ofHours(2));

        assertThat(economy.processMatch(matchId, clock.offsetNow())).isEqualTo(2);
        assertThat(economy.processMatch(matchId, clock.offsetNow())).isZero();
        assertThat(recorded.appliedTo(matchId)).hasSize(2);
    }

    @Test
    void recoversAtMostTheConfiguredNumberOfCyclesAfterDowntime() {
        String matchId = startedMatch();

        clock.advance(Duration.ofHours(30));
        assertThat(economy.processMatch(matchId, clock.offsetNow())).isEqualTo(24);

        List<AppliedCycle> applied = recorded.appliedTo(matchId);
        assertThat(applied).extracting(AppliedCycle::cycle)
                .containsExactlyElementsOf(LongStream.rangeClosed(1, 24).boxed().toList());
        // Os 6 ciclos mais antigos foram descartados; os 24 recentes mantêm seus horários.
        assertThat(applied.get(0).at()).isEqualTo(T0.plus(Duration.ofHours(7)));
        assertThat(applied.get(23).at()).isEqualTo(T0.plus(Duration.ofHours(30)));

        MatchView.Economy view = matchService.get(matchId).economy();
        assertThat(view.cycles()).isEqualTo(24);
        assertThat(view.skippedCycles()).isEqualTo(6);
        assertThat(view.nextCycleAt().toInstant()).isEqualTo(T0.plus(Duration.ofHours(31)));
    }

    @Test
    void concurrentProcessingNeverAppliesTheSameCycleTwice() throws Exception {
        String matchId = startedMatch();
        clock.advance(Duration.ofHours(5));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch go = new CountDownLatch(1);
        Callable<Long> attempt = () -> {
            go.await();
            return economy.processMatch(matchId, clock.offsetNow());
        };
        try {
            List<Future<Long>> results = List.of(pool.submit(attempt), pool.submit(attempt),
                    pool.submit(attempt), pool.submit(attempt));
            go.countDown();
            long total = 0;
            for (Future<Long> result : results) {
                total += result.get();
            }
            assertThat(total).isEqualTo(5);
        } finally {
            pool.shutdownNow();
        }
        assertThat(recorded.appliedTo(matchId)).extracting(AppliedCycle::cycle).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void openMatchesHaveNoEconomy() {
        String matchId = matchService.create(ONE, UUID.randomUUID(), "nordeste-teste", "Aberta").match().id();
        clock.advance(Duration.ofHours(3));
        economy.processDue();

        assertThat(recorded.appliedTo(matchId)).isEmpty();
        assertThat(matchService.get(matchId).economy()).isNull();
    }

    private String startedMatch() {
        String matchId = matchService.create(ONE, UUID.randomUUID(), "nordeste-teste", "Economia").match().id();
        matchService.join(ONE, UUID.randomUUID(), matchId, "parnaiba");
        matchService.join(TWO, UUID.randomUUID(), matchId, "sao-francisco");
        matchService.start(ONE, UUID.randomUUID(), matchId);
        assertThat(matchService.get(matchId).startedAt().toInstant()).isEqualTo(T0);
        return matchId;
    }
}
