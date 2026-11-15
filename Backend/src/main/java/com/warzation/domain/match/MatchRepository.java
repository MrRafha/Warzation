package com.warzation.domain.match;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, String> {

    /** Bloqueia a partida até o fim da transação, serializando entradas concorrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Match m where m.id = :id")
    Optional<Match> findByIdForUpdate(@Param("id") String id);

    List<Match> findAllByOrderByCreatedAtDesc();

    List<Match> findByStatusOrderByCreatedAtDesc(MatchStatus status);

    long countByScenarioId(String scenarioId);

    /** Partidas em andamento com ciclo econômico vencido (ou ainda sem agenda). */
    @Query("select m.id from Match m where m.status = :status"
            + " and (m.nextEconomyCycleAt is null or m.nextEconomyCycleAt <= :now) order by m.id")
    List<String> findIdsWithEconomyDue(@Param("status") MatchStatus status, @Param("now") OffsetDateTime now);
}
