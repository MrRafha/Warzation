package com.warzation.domain.match;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchCommandRepository extends JpaRepository<MatchCommand, UUID> {
}
