package com.warzation.domain.match;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchEventRepository extends JpaRepository<MatchEvent, UUID> {

    List<MatchEvent> findByMatchIdOrderBySeqAsc(String matchId);

    @Query("select coalesce(max(e.seq), 0) from MatchEvent e where e.matchId = :matchId")
    int lastSeq(@Param("matchId") String matchId);
}
