package com.warzation.domain.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Partida: estado próprio e isolado, criada a partir de um cenário. */
@Entity
@Table(name = "matches")
public class Match {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "scenario_id", nullable = false, length = 50)
    private String scenarioId;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MatchStatus status;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "next_economy_cycle_at")
    private OffsetDateTime nextEconomyCycleAt;

    @Column(name = "economy_cycles", nullable = false)
    private long economyCycles;

    @Column(name = "economy_cycles_skipped", nullable = false)
    private long economyCyclesSkipped;

    protected Match() {
    }

    public Match(String scenarioId, String name, int maxPlayers, String createdBy) {
        this.id = UUID.randomUUID().toString();
        this.scenarioId = scenarioId;
        this.name = name;
        this.status = MatchStatus.OPEN;
        this.maxPlayers = maxPlayers;
        this.createdBy = createdBy;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void touch() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * Começa a partida em {@code at}; o primeiro ciclo econômico vence um
     * ciclo depois.
     */
    public void start(OffsetDateTime at, Duration economyCycle) {
        if (status != MatchStatus.OPEN) {
            throw new IllegalStateException("Só uma partida aberta pode começar");
        }
        this.status = MatchStatus.STARTED;
        this.startedAt = at;
        this.updatedAt = at;
        this.nextEconomyCycleAt = at.plus(economyCycle);
    }

    /** Agenda o primeiro ciclo de uma partida iniciada antes do relógio econômico existir. */
    public void scheduleEconomy(OffsetDateTime nextCycleAt) {
        this.nextEconomyCycleAt = nextCycleAt;
    }

    /** Registra um ciclo aplicado e agenda o seguinte. */
    public void economyCycleApplied(Duration economyCycle) {
        this.economyCycles++;
        this.nextEconomyCycleAt = nextEconomyCycleAt.plus(economyCycle);
    }

    /** Descarta ciclos perdidos além da recuperação máxima, sem aplicá-los. */
    public void skipEconomyCycles(long count, Duration economyCycle) {
        this.economyCyclesSkipped += count;
        this.nextEconomyCycleAt = nextEconomyCycleAt.plus(economyCycle.multipliedBy(count));
    }

    public OffsetDateTime getNextEconomyCycleAt() {
        return nextEconomyCycleAt;
    }

    public long getEconomyCycles() {
        return economyCycles;
    }

    public long getEconomyCyclesSkipped() {
        return economyCyclesSkipped;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    /** Para testes e para o encerramento futuro das partidas. */
    public void changeStatus(MatchStatus status) {
        this.status = status;
        touch();
    }

    public String getId() {
        return id;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public String getName() {
        return name;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
