package com.warzation.domain.match;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MatchProvinceId implements Serializable {

    @Column(name = "match_id", nullable = false, length = 36)
    private String matchId;

    @Column(name = "province_id", nullable = false, length = 50)
    private String provinceId;

    protected MatchProvinceId() {
    }

    public MatchProvinceId(String matchId, String provinceId) {
        this.matchId = matchId;
        this.provinceId = provinceId;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getProvinceId() {
        return provinceId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MatchProvinceId id && matchId.equals(id.matchId) && provinceId.equals(id.provinceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId, provinceId);
    }
}
