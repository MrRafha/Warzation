package com.warzation.application.economy;

import com.warzation.application.rules.GameRules;
import com.warzation.domain.match.Match;
import com.warzation.domain.match.MatchRepository;
import com.warzation.domain.match.MatchStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Relógio econômico do servidor. Aplica os ciclos vencidos de cada partida em
 * andamento, com ou sem jogadores conectados.
 *
 * <p>O momento do próximo ciclo fica persistido na partida, e cada partida é
 * processada na própria transação com a linha bloqueada ({@code FOR UPDATE}).
 * Por isso dois processamentos simultâneos, no mesmo servidor ou em instâncias
 * diferentes, nunca aplicam o mesmo ciclo duas vezes, e um reinício retoma
 * exatamente de onde parou.
 *
 * <p>Depois de o servidor ficar desligado, recupera no máximo
 * {@code economy.maxCatchUpCycles} ciclos; os mais antigos são descartados e
 * contados em {@code economy_cycles_skipped}.
 */
@Service
public class EconomyClock {

    private static final Logger log = LoggerFactory.getLogger(EconomyClock.class);

    private final MatchRepository matches;
    private final GameRules rules;
    private final Clock clock;
    private final List<EconomyCycleStep> steps;
    private final TransactionTemplate transaction;

    public EconomyClock(MatchRepository matches, GameRules rules, Clock clock, ObjectProvider<EconomyCycleStep> steps,
            PlatformTransactionManager transactionManager) {
        this.matches = matches;
        this.rules = rules;
        this.clock = clock;
        this.steps = steps.orderedStream().toList();
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /** Processa todas as partidas com ciclo vencido. Devolve quantos ciclos foram aplicados. */
    public long processDue() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        long applied = 0;
        for (String matchId : matches.findIdsWithEconomyDue(MatchStatus.STARTED, now)) {
            try {
                applied += processMatch(matchId, now);
            } catch (RuntimeException exception) {
                // Uma partida com problema não pode travar a economia das outras.
                log.error("Falha ao processar a economia da partida {}", matchId, exception);
            }
        }
        return applied;
    }

    /** Processa os ciclos vencidos de uma partida até {@code now}. */
    public long processMatch(String matchId, OffsetDateTime now) {
        Long applied = transaction.execute(status -> {
            Match match = matches.findByIdForUpdate(matchId).orElse(null);
            if (match == null || match.getStatus() != MatchStatus.STARTED) {
                return 0L;
            }
            Duration cycle = rules.cycleDuration();
            if (match.getNextEconomyCycleAt() == null) {
                // Partida iniciada antes do relógio econômico existir.
                match.scheduleEconomy(now.plus(cycle));
                return 0L;
            }
            long due = dueCycles(match.getNextEconomyCycleAt(), now, cycle);
            if (due == 0) {
                return 0L;
            }
            long limit = rules.economy().maxCatchUpCycles();
            if (due > limit) {
                long skipped = due - limit;
                match.skipEconomyCycles(skipped, cycle);
                log.warn("Partida {}: {} ciclos econômicos perdidos além da recuperação máxima ({}) foram descartados",
                        matchId, skipped, limit);
                due = limit;
            }
            for (long i = 0; i < due; i++) {
                OffsetDateTime at = match.getNextEconomyCycleAt();
                long number = match.getEconomyCycles() + 1;
                for (EconomyCycleStep step : steps) {
                    step.apply(match, number, at);
                }
                match.economyCycleApplied(cycle);
            }
            return due;
        });
        return applied == null ? 0 : applied;
    }

    /** Quantos ciclos venceram entre {@code next} (inclusive) e {@code now}. */
    static long dueCycles(OffsetDateTime next, OffsetDateTime now, Duration cycle) {
        if (next.isAfter(now)) {
            return 0;
        }
        return Duration.between(next, now).toMillis() / cycle.toMillis() + 1;
    }
}
