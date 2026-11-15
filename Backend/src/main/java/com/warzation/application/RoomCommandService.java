package com.warzation.application;

import com.warzation.domain.ProcessedCommand;
import com.warzation.domain.ProcessedCommandRepository;
import com.warzation.domain.Room;
import com.warzation.domain.RoomRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Único ponto que altera o estado oficial das salas. Valida o comando, aplica
 * o efeito e registra o commandId na mesma transação: um reenvio devolve o
 * resultado gravado em vez de aplicar o efeito outra vez.
 */
@Service
public class RoomCommandService {

    private static final Pattern ROOM_ID = Pattern.compile("^[a-z0-9-]{1,50}$");

    private final RoomRepository rooms;
    private final ProcessedCommandRepository processedCommands;
    private final ApplicationEventPublisher events;

    public RoomCommandService(
            RoomRepository rooms,
            ProcessedCommandRepository processedCommands,
            ApplicationEventPublisher events) {
        this.rooms = rooms;
        this.processedCommands = processedCommands;
        this.events = events;
    }

    @Transactional
    public CommandResult execute(String roomId, RoomCommand command) {
        validate(command);
        Room room = rooms.findByIdForUpdate(requireValidRoomId(roomId))
                .orElseThrow(() -> roomNotFound(roomId));

        Optional<ProcessedCommand> previous = processedCommands.findById(command.commandId());
        if (previous.isPresent()) {
            ProcessedCommand processed = previous.get();
            if (!processed.sameIntent(roomId, command.type().name(), command.amount())) {
                throw new CommandConflictException(
                        "O commandId " + command.commandId() + " já foi usado por outro comando");
            }
            return CommandResult.of(processed, CommandResult.Status.DUPLICATE, RoomState.from(room));
        }

        switch (command.type()) {
            case INCREMENT_COUNTER -> room.incrementCounter(command.amount());
        }

        ProcessedCommand processed = processedCommands.save(new ProcessedCommand(
                command.commandId(),
                room.getId(),
                command.type().name(),
                command.amount(),
                room.getCounterValue(),
                room.getVersion()));
        RoomState state = RoomState.from(room);
        events.publishEvent(new RoomStateChanged(state));
        return CommandResult.of(processed, CommandResult.Status.APPLIED, state);
    }

    @Transactional(readOnly = true)
    public RoomState getState(String roomId) {
        return rooms.findById(requireValidRoomId(roomId))
                .map(RoomState::from)
                .orElseThrow(() -> roomNotFound(roomId));
    }

    @Transactional(readOnly = true)
    public CommandResult findResult(String roomId, UUID commandId) {
        RoomState state = getState(roomId);
        return processedCommands.findById(commandId)
                .filter(processed -> processed.getRoomId().equals(roomId))
                .map(processed -> CommandResult.of(processed, CommandResult.Status.APPLIED, state))
                .orElseThrow(() -> new ResourceNotFoundException("Comando não encontrado: " + commandId));
    }

    private static void validate(RoomCommand command) {
        if (command == null || command.commandId() == null || command.type() == null) {
            throw new InvalidCommandException("Comando incompleto", Map.of());
        }
        if (command.amount() < RoomCommand.MIN_AMOUNT || command.amount() > RoomCommand.MAX_AMOUNT) {
            throw new InvalidCommandException("Comando inválido", Map.of(
                    "payload.amount",
                    "deve estar entre " + RoomCommand.MIN_AMOUNT + " e " + RoomCommand.MAX_AMOUNT));
        }
    }

    private static String requireValidRoomId(String roomId) {
        if (roomId == null || !ROOM_ID.matcher(roomId).matches()) {
            throw roomNotFound(roomId);
        }
        return roomId;
    }

    private static ResourceNotFoundException roomNotFound(String roomId) {
        return new ResourceNotFoundException("Sala não encontrada: " + roomId);
    }
}
