package com.warzation.realtime;

import com.warzation.application.RoomState;
import com.warzation.application.RoomStateChanged;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.WebSocketSession;

/**
 * Publica o novo estado oficial para todos os clientes da sala, somente
 * depois que a transação que o alterou foi confirmada no banco.
 */
@Component
public class RoomStateBroadcaster {

    private final RoomSessionRegistry registry;
    private final RealtimeSender sender;

    public RoomStateBroadcaster(RoomSessionRegistry registry, RealtimeSender sender) {
        this.registry = registry;
        this.sender = sender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomStateChanged(RoomStateChanged event) {
        RoomState state = event.state();
        ServerMessage message = ServerMessage.of(ServerMessage.Types.ROOM_STATE, null, state.roomId(), state);
        for (WebSocketSession session : registry.sessionsIn(state.roomId())) {
            sender.send(session, message);
        }
    }
}
