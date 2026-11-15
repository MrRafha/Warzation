package com.warzation.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warzation.application.CommandConflictException;
import com.warzation.application.CommandResult;
import com.warzation.application.InvalidCommandException;
import com.warzation.application.ResourceNotFoundException;
import com.warzation.application.RoomCommandService;
import com.warzation.application.RoomState;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Gateway WebSocket em {@code /ws}. Contrato documentado em
 * {@code Docs/03_ARQUITETURA_E_STACK.md}, seção "Contrato do WebSocket".
 */
@Component
public class RoomWebSocketHandler extends TextWebSocketHandler {

    static final int MAX_MESSAGE_CHARS = 8 * 1024;

    private static final Logger log = LoggerFactory.getLogger(RoomWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final RoomSessionRegistry registry;
    private final RealtimeSender sender;
    private final RoomCommandService commandService;
    private final RoomCommandMapper commandMapper;

    public RoomWebSocketHandler(
            ObjectMapper objectMapper,
            RoomSessionRegistry registry,
            RealtimeSender sender,
            RoomCommandService commandService,
            RoomCommandMapper commandMapper) {
        this.objectMapper = objectMapper;
        this.registry = registry;
        this.sender = sender;
        this.commandService = commandService;
        this.commandMapper = commandMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession safe = registry.register(session);
        sender.send(safe, ServerMessage.of(ServerMessage.Types.CONNECTED, null, null,
                Map.of("sessionId", session.getId())));
    }

    @Override
    protected void handleTextMessage(WebSocketSession rawSession, TextMessage message) throws IOException {
        WebSocketSession session = registry.get(rawSession.getId()).orElse(rawSession);

        if (message.getPayloadLength() > MAX_MESSAGE_CHARS) {
            log.warn("Sessão {} enviou mensagem acima do limite ({} caracteres)",
                    session.getId(), message.getPayloadLength());
            session.close(CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }

        JsonNode envelope;
        try {
            envelope = objectMapper.readTree(message.getPayload());
        } catch (JsonProcessingException exception) {
            reject(session, null, "INVALID_MESSAGE", "A mensagem não é um JSON válido", Map.of());
            return;
        }

        if (envelope == null || !envelope.isObject() || !textField(envelope, "type").isPresent()) {
            reject(session, null, "INVALID_MESSAGE", "A mensagem precisa ser um objeto com 'type'", Map.of());
            return;
        }

        String type = textField(envelope, "type").get();
        String messageId = textField(envelope, "id").orElse(null);
        JsonNode payload = envelope.get("payload");

        switch (type) {
            case "JOIN_ROOM" -> joinRoom(session, messageId, payload);
            case "LEAVE_ROOM" -> leaveRoom(session, messageId);
            case "COMMAND" -> executeCommand(session, messageId, payload);
            case "PING" -> sender.send(session, ServerMessage.of(ServerMessage.Types.PONG, messageId, null, null));
            default -> reject(session, messageId, "UNKNOWN_TYPE", "Tipo de mensagem desconhecido: " + type, Map.of());
        }
    }

    private void joinRoom(WebSocketSession session, String messageId, JsonNode payload) {
        Optional<String> roomId = payload == null ? Optional.empty() : textField(payload, "roomId");
        if (roomId.isEmpty()) {
            reject(session, messageId, "INVALID_MESSAGE", "JOIN_ROOM exige payload.roomId",
                    Map.of("payload.roomId", "obrigatório"));
            return;
        }
        try {
            RoomState state = commandService.getState(roomId.get());
            registry.join(session.getId(), state.roomId());
            sender.send(session, ServerMessage.of(ServerMessage.Types.ROOM_STATE, messageId, state.roomId(), state));
        } catch (ResourceNotFoundException exception) {
            reject(session, messageId, "ROOM_NOT_FOUND", exception.getMessage(), Map.of());
        }
    }

    private void leaveRoom(WebSocketSession session, String messageId) {
        String roomId = registry.leave(session.getId()).orElse(null);
        sender.send(session, ServerMessage.of(ServerMessage.Types.LEFT_ROOM, messageId, roomId, null));
    }

    private void executeCommand(WebSocketSession session, String messageId, JsonNode payload) {
        Optional<String> roomId = registry.roomOf(session.getId());
        if (roomId.isEmpty()) {
            reject(session, messageId, "NOT_IN_ROOM", "Entre em uma sala com JOIN_ROOM antes de enviar comandos",
                    Map.of());
            return;
        }
        try {
            CommandResult result = commandService.execute(roomId.get(), commandMapper.toCommand(payload));
            sender.send(session, ServerMessage.of(ServerMessage.Types.COMMAND_RESULT, messageId, roomId.get(), result));
        } catch (InvalidCommandException exception) {
            reject(session, messageId, "INVALID_COMMAND", exception.getMessage(), exception.getFields());
        } catch (CommandConflictException exception) {
            reject(session, messageId, "COMMAND_CONFLICT", exception.getMessage(), Map.of());
        } catch (ResourceNotFoundException exception) {
            reject(session, messageId, "ROOM_NOT_FOUND", exception.getMessage(), Map.of());
        } catch (RuntimeException exception) {
            log.error("Erro inesperado ao processar comando da sessão {}", session.getId(), exception);
            sender.send(session, ServerMessage.of(ServerMessage.Types.ERROR, messageId, roomId.get(),
                    ErrorPayload.of("INTERNAL_ERROR", "Ocorreu um erro interno. Tente novamente.")));
        }
    }

    private void reject(WebSocketSession session, String messageId, String code, String text,
            Map<String, String> fields) {
        log.warn("Mensagem rejeitada da sessão {}: {} - {}", session.getId(), code, text);
        sender.send(session, ServerMessage.of(ServerMessage.Types.ERROR, messageId, null,
                new ErrorPayload(code, text, fields)));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("Erro de transporte na sessão {}: {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.remove(session.getId());
    }

    private static Optional<String> textField(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.asText());
    }
}
