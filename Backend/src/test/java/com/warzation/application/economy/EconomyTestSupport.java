package com.warzation.application.economy;

import com.warzation.domain.match.Match;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Relógio controlado e etapa que registra os ciclos, para testar o relógio econômico. */
@TestConfiguration(proxyBeanMethods = false)
public class EconomyTestSupport {

    /** Início fixo das partidas nos testes. */
    public static final Instant T0 = Instant.parse("2026-11-16T12:00:00Z");

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return new MutableClock(T0);
    }

    @Bean
    public RecordingStep recordingStep() {
        return new RecordingStep();
    }

    /** Relógio que só anda quando o teste manda. */
    public static class MutableClock extends Clock {

        private volatile Instant now;

        public MutableClock(Instant now) {
            this.now = now;
        }

        public void set(Instant instant) {
            this.now = instant;
        }

        public void advance(Duration duration) {
            this.now = now.plus(duration);
        }

        public OffsetDateTime offsetNow() {
            return OffsetDateTime.ofInstant(now, ZoneOffset.UTC);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    public record AppliedCycle(String matchId, long cycle, Instant at) {
    }

    /** Registra cada ciclo aplicado, para conferir número e momento. */
    public static class RecordingStep implements EconomyCycleStep {

        private final List<AppliedCycle> applied = new CopyOnWriteArrayList<>();

        @Override
        public void apply(Match match, long cycle, OffsetDateTime at) {
            applied.add(new AppliedCycle(match.getId(), cycle, at.toInstant()));
        }

        public List<AppliedCycle> appliedTo(String matchId) {
            return applied.stream().filter(a -> a.matchId().equals(matchId)).toList();
        }
    }
}
