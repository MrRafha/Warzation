package com.warzation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Sala de teste da Sprint 1: representa o estado mínimo e oficial de uma
 * partida. Só o backend altera o contador, aplicando comandos validados.
 */
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @Column(name = "counter_value", nullable = false)
    private long counterValue;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Room() {
    }

    public Room(String id) {
        this.id = id;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void incrementCounter(long amount) {
        this.counterValue = Math.addExact(this.counterValue, amount);
        this.version++;
        this.updatedAt = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public long getCounterValue() {
        return counterValue;
    }

    public long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
