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

    @Column(name = "food_shortage_last_cycle", nullable = false)
    private long foodShortageLastCycle;

    @Column(name = "food_shortage_total", nullable = false)
    private long foodShortageTotal;

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

    /** Registra quanto faltou de comida no ciclo (0 = abastecida). */
    public void recordFoodShortage(long shortage) {
        if (shortage < 0) {
            throw new IllegalArgumentException("Falta de comida não pode ser negativa");
        }
        this.foodShortageLastCycle = shortage;
        this.foodShortageTotal += shortage;
    }

    /** Faltou comida no último ciclo. */
    public boolean inFoodDeficit() {
        return foodShortageLastCycle > 0;
    }

    public long getFoodShortageLastCycle() {
        return foodShortageLastCycle;
    }

    public long getFoodShortageTotal() {
        return foodShortageTotal;
    }
}
