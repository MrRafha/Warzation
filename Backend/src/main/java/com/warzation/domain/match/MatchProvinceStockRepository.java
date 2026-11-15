package com.warzation.domain.match;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchProvinceStockRepository extends JpaRepository<MatchProvinceStock, MatchProvinceStockId> {

    List<MatchProvinceStock> findByIdMatchId(String matchId);
}
