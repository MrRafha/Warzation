package com.warzation.domain.match;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** Jogador que entrou numa partida com uma nação. */
@Entity
@Table(name = "match_participants")
public class MatchParticipant {

    @EmbeddedId
    private MatchParticipantId id;

    @Column(name = "scenario_id", nullable = false, length = 50)
    private String scenarioId;

    @Column(name = "nation_id", nullable = false, length = 50)
    private String nationId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    protected MatchParticipant() {
    }

    public MatchParticipant(Match match, String playerId, String nationId) {
        this.id = new MatchParticipantId(match.getId(), playerId);
        this.scenarioId = match.getScenarioId();
        this.nationId = nationId;
        this.joinedAt = OffsetDateTime.now();
    }

    public String getMatchId() {
        return id.getMatchId();
    }

    public String getPlayerId() {
        return id.getPlayerId();
    }

    public String getNationId() {
        return nationId;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }
}
