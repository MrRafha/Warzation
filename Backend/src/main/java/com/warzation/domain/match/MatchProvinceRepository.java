package com.warzation.domain.match;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchProvinceRepository extends JpaRepository<MatchProvince, MatchProvinceId> {

    List<MatchProvince> findByIdMatchId(String matchId);
}
