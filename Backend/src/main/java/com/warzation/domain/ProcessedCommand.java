package com.warzation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Registro persistido de um comando já aplicado. A chave é o identificador
 * enviado pelo cliente, o que torna reenvios idempotentes mesmo após
 * reinicializações do servidor.
 */
@Entity
@Table(name = "processed_commands")
public class ProcessedCommand {

    @Id
    @Column(name = "command_id", nullable = false)
    private UUID commandId;

    @Column(name = "room_id", nullable = false, length = 50)
    private String roomId;

    @Column(name = "command_type", nullable = false, length = 50)
    private String commandType;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "resulting_counter", nullable = false)
    private long resultingCounter;

    @Column(name = "resulting_version", nullable = false)
    private long resultingVersion;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;

    protected ProcessedCommand() {
    }

    public ProcessedCommand(
            UUID commandId,
            String roomId,
            String commandType,
            long amount,
            long resultingCounter,
            long resultingVersion) {
        this.commandId = commandId;
        this.roomId = roomId;
        this.commandType = commandType;
        this.amount = amount;
        this.resultingCounter = resultingCounter;
        this.resultingVersion = resultingVersion;
        this.processedAt = OffsetDateTime.now();
    }

    public boolean sameIntent(String roomId, String commandType, long amount) {
        return this.roomId.equals(roomId) && this.commandType.equals(commandType) && this.amount == amount;
    }

    public UUID getCommandId() {
        return commandId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getCommandType() {
        return commandType;
    }

    public long getAmount() {
        return amount;
    }

    public long getResultingCounter() {
        return resultingCounter;
    }

    public long getResultingVersion() {
        return resultingVersion;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
