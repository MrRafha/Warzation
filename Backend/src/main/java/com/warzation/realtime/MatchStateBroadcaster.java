package com.warzation.realtime;

import com.warzation.application.match.MatchChanged;
import com.warzation.application.match.MatchService;
import com.warzation.application.match.MatchSnapshot;
import java.util.Collection;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.WebSocketSession;

/**
 * Envia o estado oficial da partida a todos os participantes conectados ao
 * canal dela, só depois que a mudança foi confirmada no banco.
 */
@Component
public class MatchStateBroadcaster {

    /** Canal de uma partida no registro de sessões (separado das salas de teste). */
    public static String channel(String matchId) {
        return "match:" + matchId;
    }

    private final RoomSessionRegistry registry;
    private final RealtimeSender sender;
    private final MatchService matchService;

    public MatchStateBroadcaster(RoomSessionRegistry registry, RealtimeSender sender, MatchService matchService) {
        this.registry = registry;
        this.sender = sender;
        this.matchService = matchService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchChanged(MatchChanged event) {
        Collection<WebSocketSession> sessions = registry.sessionsIn(channel(event.matchId()));
        if (sessions.isEmpty()) {
            return;
        }
        MatchSnapshot snapshot = matchService.snapshot(event.matchId());
        ServerMessage message = ServerMessage.of(ServerMessage.Types.MATCH_STATE, null, event.matchId(), snapshot);
        for (WebSocketSession session : sessions) {
            sender.send(session, message);
        }
    }
}
