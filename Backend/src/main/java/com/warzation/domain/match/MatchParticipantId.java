package com.warzation.domain.match;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MatchParticipantId implements Serializable {

    @Column(name = "match_id", nullable = false, length = 36)
    private String matchId;

    @Column(name = "player_id", nullable = false, length = 50)
    private String playerId;

    protected MatchParticipantId() {
    }

    public MatchParticipantId(String matchId, String playerId) {
        this.matchId = matchId;
        this.playerId = playerId;
    }

    public String getMatchId() {
        return matchId;
    }

    public String getPlayerId() {
        return playerId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MatchParticipantId id
                && matchId.equals(id.matchId)
                && playerId.equals(id.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId, playerId);
    }
}
