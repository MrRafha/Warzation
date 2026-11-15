package com.warzation.application.economy;

import com.warzation.application.rules.GameRules;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Dispara o relógio econômico periodicamente: a cada minuto (Docs/01, seção 10)
 * ou a cada ciclo, se o ciclo for mais curto (GAME_SPEED alto). Desligado nos
 * testes automatizados, que avançam o tempo e chamam {@link EconomyClock}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "application.economy.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class EconomyScheduler implements SchedulingConfigurer {

    static final Duration MAX_TICK = Duration.ofMinutes(1);
    private static final Logger log = LoggerFactory.getLogger(EconomyScheduler.class);

    private final EconomyClock economy;
    private final GameRules rules;

    public EconomyScheduler(EconomyClock economy, GameRules rules) {
        this.economy = economy;
        this.rules = rules;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        Duration tick = tick(rules.cycleDuration());
        log.info("Relógio econômico verificando ciclos a cada {}", tick);
        registrar.addFixedDelayTask(this::run, tick);
    }

    static Duration tick(Duration cycle) {
        return cycle.compareTo(MAX_TICK) < 0 ? cycle : MAX_TICK;
    }

    private void run() {
        long applied = economy.processDue();
        if (applied > 0) {
            log.debug("{} ciclos econômicos aplicados", applied);
        }
    }
}
