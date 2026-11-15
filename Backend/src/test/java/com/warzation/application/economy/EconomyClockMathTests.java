package com.warzation.application.economy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class EconomyClockMathTests {

    private static final OffsetDateTime NEXT = OffsetDateTime.parse("2026-11-16T13:00:00Z");
    private static final Duration HOUR = Duration.ofHours(1);

    @Test
    void nothingIsDueBeforeTheNextCycle() {
        assertThat(EconomyClock.dueCycles(NEXT, NEXT.minusSeconds(1), HOUR)).isZero();
    }

    @Test
    void theCycleIsDueExactlyAtItsTime() {
        assertThat(EconomyClock.dueCycles(NEXT, NEXT, HOUR)).isEqualTo(1);
    }

    @Test
    void countsEveryCycleThatAlreadyExpired() {
        assertThat(EconomyClock.dueCycles(NEXT, NEXT.plusMinutes(59), HOUR)).isEqualTo(1);
        assertThat(EconomyClock.dueCycles(NEXT, NEXT.plusHours(2), HOUR)).isEqualTo(3);
    }

    @Test
    void schedulerTicksEveryMinuteOrEveryCycleIfShorter() {
        assertThat(EconomyScheduler.tick(HOUR)).isEqualTo(Duration.ofMinutes(1));
        assertThat(EconomyScheduler.tick(Duration.ofSeconds(1))).isEqualTo(Duration.ofSeconds(1));
    }
}
