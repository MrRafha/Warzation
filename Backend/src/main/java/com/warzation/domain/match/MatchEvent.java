package com.warzation.domain.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Fato registrado no histórico da partida. Nunca é alterado depois de gravado. */
@Entity
@Table(name = "match_events")
public class MatchEvent {

    public static final String MATCH_CREATED = "MATCH_CREATED";
    public static final String PLAYER_JOINED = "PLAYER_JOINED";
    public static final String MATCH_STARTED = "MATCH_STARTED";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "match_id", nullable = false, length = 36)
    private String matchId;

    @Column(name = "seq", nullable = false)
    private int seq;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "player_id", length = 50)
    private String playerId;

    @Column(name = "command_id")
    private UUID commandId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    /** Dados do evento em JSON. Nunca contém tokens. */
    @Column(name = "data", nullable = false, length = 4000)
    private String data;

    protected MatchEvent() {
    }

    public MatchEvent(String matchId, int seq, String eventType, String playerId, UUID commandId, String data) {
        this.id = UUID.randomUUID();
        this.matchId = matchId;
        this.seq = seq;
        this.eventType = eventType;
        this.playerId = playerId;
        this.commandId = commandId;
        this.occurredAt = OffsetDateTime.now();
        this.data = data;
    }

    public int getSeq() {
        return seq;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPlayerId() {
        return playerId;
    }

    public UUID getCommandId() {
        return commandId;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getData() {
        return data;
    }
}
