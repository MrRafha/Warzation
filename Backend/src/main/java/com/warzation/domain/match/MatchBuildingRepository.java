package com.warzation.domain.match;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchBuildingRepository extends JpaRepository<MatchBuilding, UUID> {

    List<MatchBuilding> findByMatchIdOrderByStartedAtAsc(String matchId);

    List<MatchBuilding> findByMatchIdAndStatus(String matchId, MatchBuilding.Status status);
}
