package com.warzation.domain.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Construção numa província da partida: em obra ou concluída. Sem níveis. */
@Entity
@Table(name = "match_buildings")
public class MatchBuilding {

    public enum Status {
        UNDER_CONSTRUCTION,
        COMPLETED
    }

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "match_id", nullable = false, length = 36)
    private String matchId;

    @Column(name = "province_id", nullable = false, length = 50)
    private String provinceId;

    /** Id da construção em game-rules.json (FAZENDA, MINA...). */
    @Column(name = "building_type", nullable = false, length = 30)
    private String buildingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completes_at", nullable = false)
    private OffsetDateTime completesAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected MatchBuilding() {
    }

    private MatchBuilding(String matchId, String provinceId, String buildingType, OffsetDateTime startedAt,
            OffsetDateTime completesAt) {
        this.id = UUID.randomUUID();
        this.matchId = matchId;
        this.provinceId = provinceId;
        this.buildingType = buildingType;
        this.status = Status.UNDER_CONSTRUCTION;
        this.startedAt = startedAt;
        this.completesAt = completesAt;
    }

    public static MatchBuilding underConstruction(String matchId, String provinceId, String buildingType,
            OffsetDateTime startedAt, OffsetDateTime completesAt) {
        return new MatchBuilding(matchId, provinceId, buildingType, startedAt, completesAt);
    }

    /** Construção já pronta em {@code at} (testes e conclusão de obras). */
    public static MatchBuilding completed(String matchId, String provinceId, String buildingType, OffsetDateTime at) {
        MatchBuilding building = new MatchBuilding(matchId, provinceId, buildingType, at, at);
        building.complete(at);
        return building;
    }

    public void complete(OffsetDateTime at) {
        this.status = Status.COMPLETED;
        this.completedAt = at;
    }

    /** Pronta e em funcionamento no instante {@code at}. */
    public boolean operatingAt(OffsetDateTime at) {
        return status == Status.COMPLETED && !completedAt.isAfter(at);
    }

    public UUID getId() {
        return id;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getProvinceId() {
        return provinceId;
    }

    public String getBuildingType() {
        return buildingType;
    }

    public Status getStatus() {
        return status;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getCompletesAt() {
        return completesAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
}
