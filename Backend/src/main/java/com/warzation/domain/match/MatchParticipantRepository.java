package com.warzation.domain.match;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, MatchParticipantId> {

    List<MatchParticipant> findByIdMatchIdOrderByJoinedAtAsc(String matchId);
}
