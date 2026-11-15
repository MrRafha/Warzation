package com.warzation.application;

import com.warzation.domain.ProcessedCommand;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resultado consultável de um comando.
 *
 * <p>{@code status} é {@code APPLIED} na primeira execução e {@code DUPLICATE}
 * quando o mesmo {@code commandId} é reenviado; nesse caso nada é reaplicado.
 * {@code resultingCounter}/{@code resultingVersion} registram o estado logo
 * após o comando; {@code room} é o estado atual da sala.</p>
 */
public record CommandResult(
        UUID commandId,
        String roomId,
        String type,
        long amount,
        Status status,
        OffsetDateTime processedAt,
        long resultingCounter,
        long resultingVersion,
        RoomState room) {

    public enum Status {
        APPLIED,
        DUPLICATE
    }

    static CommandResult of(ProcessedCommand command, Status status, RoomState room) {
        return new CommandResult(
                command.getCommandId(),
                command.getRoomId(),
                command.getCommandType(),
                command.getAmount(),
                status,
                command.getProcessedAt(),
                command.getResultingCounter(),
                command.getResultingVersion(),
                room);
    }
}
