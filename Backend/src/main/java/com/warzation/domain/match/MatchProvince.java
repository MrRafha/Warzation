package com.warzation.domain.match;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Província dentro de uma partida: dono atual (nação) ou neutra. */
@Entity
@Table(name = "match_provinces")
public class MatchProvince {

    @EmbeddedId
    private MatchProvinceId id;

    @Column(name = "scenario_id", nullable = false, length = 50)
    private String scenarioId;

    @Column(name = "owner_nation_id", length = 50)
    private String ownerNationId;

    protected MatchProvince() {
    }

    public MatchProvince(String matchId, String scenarioId, String provinceId, String ownerNationId) {
        this.id = new MatchProvinceId(matchId, provinceId);
        this.scenarioId = scenarioId;
        this.ownerNationId = ownerNationId;
    }

    public String getMatchId() {
        return id.getMatchId();
    }

    public String getProvinceId() {
        return id.getProvinceId();
    }

    public String getOwnerNationId() {
        return ownerNationId;
    }
}
