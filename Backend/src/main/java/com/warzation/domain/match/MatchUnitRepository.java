package com.warzation.domain.match;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchUnitRepository extends JpaRepository<MatchUnit, UUID> {

    List<MatchUnit> findByMatchId(String matchId);
}
