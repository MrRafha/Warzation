package com.warzation.realtime;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.warzation.application.player.Player;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/**
 * Sessões WebSocket conectadas e a sala em que cada uma está. Guarda apenas
 * quem deve receber atualizações; nenhum estado de jogo fica aqui.
 */
@Component
public class RoomSessionRegistry {

    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 256 * 1024;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> roomBySession = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionsByRoom = new ConcurrentHashMap<>();
    private final Map<String, Player> playerBySession = new ConcurrentHashMap<>();
    private final Map<String, Integer> failedAuthentications = new ConcurrentHashMap<>();

    /** Registra a sessão e devolve uma versão segura para envios concorrentes. */
    public WebSocketSession register(WebSocketSession session) {
        WebSocketSession decorated =
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);
        sessions.put(session.getId(), decorated);
        return decorated;
    }

    public Optional<WebSocketSession> get(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public synchronized void join(String sessionId, String roomId) {
        leave(sessionId);
        roomBySession.put(sessionId, roomId);
        sessionsByRoom.computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    public synchronized Optional<String> leave(String sessionId) {
        String roomId = roomBySession.remove(sessionId);
        if (roomId != null) {
            sessionsByRoom.computeIfPresent(roomId, (key, members) -> {
                members.remove(sessionId);
                return members.isEmpty() ? null : members;
            });
        }
        return Optional.ofNullable(roomId);
    }

    public Optional<String> roomOf(String sessionId) {
        return Optional.ofNullable(roomBySession.get(sessionId));
    }

    public Collection<WebSocketSession> sessionsIn(String roomId) {
        Set<String> members = sessionsByRoom.get(roomId);
        if (members == null) {
            return List.of();
        }
        return members.stream().map(sessions::get).filter(session -> session != null).toList();
    }

    public void authenticate(String sessionId, Player player) {
        playerBySession.put(sessionId, player);
        failedAuthentications.remove(sessionId);
    }

    public Optional<Player> playerOf(String sessionId) {
        return Optional.ofNullable(playerBySession.get(sessionId));
    }

    /** Registra uma tentativa falha e devolve quantas já houve nesta sessão. */
    public int recordFailedAuthentication(String sessionId) {
        return failedAuthentications.merge(sessionId, 1, Integer::sum);
    }

    public void remove(String sessionId) {
        leave(sessionId);
        sessions.remove(sessionId);
        playerBySession.remove(sessionId);
        failedAuthentications.remove(sessionId);
    }
}
