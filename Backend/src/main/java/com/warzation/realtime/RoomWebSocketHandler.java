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
import com.warzation.application.match.MatchRuleException;
import com.warzation.application.match.MatchService;
import com.warzation.application.match.MatchSnapshot;
import com.warzation.application.player.Player;
import com.warzation.application.player.PlayerDirectory;
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
    static final int MAX_FAILED_AUTHENTICATIONS = 3;

    private static final Logger log = LoggerFactory.getLogger(RoomWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final RoomSessionRegistry registry;
    private final RealtimeSender sender;
    private final RoomCommandService commandService;
    private final RoomCommandMapper commandMapper;
    private final PlayerDirectory players;
    private final MatchService matchService;

    public RoomWebSocketHandler(
            ObjectMapper objectMapper,
            RoomSessionRegistry registry,
            RealtimeSender sender,
            RoomCommandService commandService,
            RoomCommandMapper commandMapper,
            PlayerDirectory players,
            MatchService matchService) {
        this.objectMapper = objectMapper;
        this.registry = registry;
        this.sender = sender;
        this.commandService = commandService;
        this.commandMapper = commandMapper;
        this.players = players;
        this.matchService = matchService;
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
            case "AUTHENTICATE" -> authenticate(session, messageId, payload);
            case "PING" -> sender.send(session, ServerMessage.of(ServerMessage.Types.PONG, messageId, null, null));
            case "JOIN_ROOM", "LEAVE_ROOM", "COMMAND", "JOIN_MATCH" -> {
                Optional<Player> player = registry.playerOf(session.getId());
                if (player.isEmpty()) {
                    reject(session, messageId, "UNAUTHENTICATED",
                            "Envie AUTHENTICATE com o token do jogador antes de " + type, Map.of());
                    return;
                }
                switch (type) {
                    case "JOIN_MATCH" -> joinMatch(session, messageId, player.get(), payload);
                    case "JOIN_ROOM" -> joinRoom(session, messageId, payload);
                    case "LEAVE_ROOM" -> leaveRoom(session, messageId);
                    default -> executeCommand(session, messageId, player.get(), payload);
                }
            }
            default -> reject(session, messageId, "UNKNOWN_TYPE", "Tipo de mensagem desconhecido: " + type, Map.of());
        }
    }

    private void authenticate(WebSocketSession session, String messageId, JsonNode payload) throws IOException {
        String token = payload == null ? null : textField(payload, "token").orElse(null);
        Optional<Player> player = players.authenticate(token);
        if (player.isEmpty()) {
            int failures = registry.recordFailedAuthentication(session.getId());
            reject(session, messageId, "UNAUTHENTICATED", "Token de jogador inválido", Map.of());
            if (failures >= MAX_FAILED_AUTHENTICATIONS) {
                log.warn("Sessão {} encerrada após {} tentativas de identificação inválidas", session.getId(), failures);
                session.close(CloseStatus.POLICY_VIOLATION);
            }
            return;
        }
        Optional<Player> current = registry.playerOf(session.getId());
        if (current.isPresent() && !current.get().id().equals(player.get().id())) {
            reject(session, messageId, "ALREADY_AUTHENTICATED",
                    "Esta conexão já pertence a " + current.get().id() + "; abra outra conexão para trocar de jogador",
                    Map.of());
            return;
        }
        registry.authenticate(session.getId(), player.get());
        sender.send(session, ServerMessage.of(ServerMessage.Types.AUTHENTICATED, messageId, null, player.get()));
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

    /** Entra no canal de uma partida; só participantes. Responde com o estado completo. */
    private void joinMatch(WebSocketSession session, String messageId, Player player, JsonNode payload) {
        Optional<String> matchId = payload == null ? Optional.empty() : textField(payload, "matchId");
        if (matchId.isEmpty()) {
            reject(session, messageId, "INVALID_MESSAGE", "JOIN_MATCH exige payload.matchId",
                    Map.of("payload.matchId", "obrigatório"));
            return;
        }
        try {
            MatchSnapshot snapshot = matchService.snapshotFor(player, matchId.get());
            registry.join(session.getId(), MatchStateBroadcaster.channel(matchId.get()));
            sender.send(session, ServerMessage.of(ServerMessage.Types.MATCH_STATE, messageId, matchId.get(), snapshot));
        } catch (ResourceNotFoundException exception) {
            reject(session, messageId, "MATCH_NOT_FOUND", exception.getMessage(), Map.of());
        } catch (MatchRuleException exception) {
            reject(session, messageId, exception.getReason().name(), exception.getMessage(), Map.of());
        }
    }

    private void leaveRoom(WebSocketSession session, String messageId) {
        String roomId = registry.leave(session.getId()).orElse(null);
        sender.send(session, ServerMessage.of(ServerMessage.Types.LEFT_ROOM, messageId, roomId, null));
    }

    private void executeCommand(WebSocketSession session, String messageId, Player player, JsonNode payload) {
        Optional<String> roomId = registry.roomOf(session.getId());
        if (roomId.isEmpty()) {
            reject(session, messageId, "NOT_IN_ROOM", "Entre em uma sala com JOIN_ROOM antes de enviar comandos",
                    Map.of());
            return;
        }
        try {
            CommandResult result = commandService.execute(roomId.get(), player.id(), commandMapper.toCommand(payload));
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
