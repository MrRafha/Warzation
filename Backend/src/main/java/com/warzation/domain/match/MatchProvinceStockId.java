package com.warzation.domain.match;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MatchProvinceStockId implements Serializable {

    @Column(name = "match_id", nullable = false, length = 36)
    private String matchId;

    @Column(name = "province_id", nullable = false, length = 50)
    private String provinceId;

    @Column(name = "resource", nullable = false, length = 20)
    private String resource;

    protected MatchProvinceStockId() {
    }

    public MatchProvinceStockId(String matchId, String provinceId, String resource) {
        this.matchId = matchId;
        this.provinceId = provinceId;
        this.resource = resource;
    }

    public String getProvinceId() {
        return provinceId;
    }

    public String getResource() {
        return resource;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MatchProvinceStockId id
                && matchId.equals(id.matchId) && provinceId.equals(id.provinceId) && resource.equals(id.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId, provinceId, resource);
    }
}
