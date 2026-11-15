package com.warzation.domain.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** Comando de partida já aplicado; garante que reenvios não repitam o efeito. */
@Entity
@Table(name = "match_commands")
public class MatchCommand {

    public static final String CREATE_MATCH = "CREATE_MATCH";
    public static final String JOIN_MATCH = "JOIN_MATCH";

    @Id
    @Column(name = "command_id", nullable = false)
    private UUID commandId;

    @Column(name = "player_id", nullable = false, length = 50)
    private String playerId;

    @Column(name = "command_type", nullable = false, length = 30)
    private String commandType;

    @Column(name = "match_id", nullable = false, length = 36)
    private String matchId;

    @Column(name = "nation_id", length = 50)
    private String nationId;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;

    protected MatchCommand() {
    }

    public MatchCommand(UUID commandId, String playerId, String commandType, String matchId, String nationId) {
        this.commandId = commandId;
        this.playerId = playerId;
        this.commandType = commandType;
        this.matchId = matchId;
        this.nationId = nationId;
        this.processedAt = OffsetDateTime.now();
    }

    public boolean sameIntent(String playerId, String commandType, String matchId, String nationId) {
        return this.playerId.equals(playerId)
                && this.commandType.equals(commandType)
                && (matchId == null || this.matchId.equals(matchId))
                && Objects.equals(this.nationId, nationId);
    }

    public String getMatchId() {
        return matchId;
    }
}
