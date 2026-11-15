package com.warzation.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.warzation.application.CommandResult;
import com.warzation.application.InvalidCommandException;
import com.warzation.application.RoomCommandService;
import com.warzation.application.RoomState;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API HTTP da sala de teste. Não existe rota para gravar o estado
 * diretamente: o cliente só envia comandos, que o servidor valida e aplica.
 */
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomCommandService commandService;
    private final RoomCommandMapper commandMapper;

    public RoomController(RoomCommandService commandService, RoomCommandMapper commandMapper) {
        this.commandService = commandService;
        this.commandMapper = commandMapper;
    }

    @GetMapping("/{roomId}")
    public RoomState getRoom(@PathVariable String roomId) {
        return commandService.getState(roomId);
    }

    @PostMapping("/{roomId}/commands")
    public ResponseEntity<CommandResult> executeCommand(
            @PathVariable String roomId,
            @RequestBody JsonNode body) {
        CommandResult result = commandService.execute(roomId, commandMapper.toCommand(body));
        HttpStatus status = result.status() == CommandResult.Status.APPLIED ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result);
    }

    @GetMapping("/{roomId}/commands/{commandId}")
    public CommandResult getCommandResult(@PathVariable String roomId, @PathVariable String commandId) {
        UUID id = RoomCommandMapper.parseUuid(commandId);
        if (id == null) {
            throw new InvalidCommandException("Comando inválido", Map.of("commandId", "deve ser um UUID válido"));
        }
        return commandService.findResult(roomId, id);
    }
}
