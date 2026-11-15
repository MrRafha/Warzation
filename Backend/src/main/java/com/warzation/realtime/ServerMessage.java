package com.warzation.realtime;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

/**
 * Envelope de toda mensagem enviada pelo servidor via WebSocket.
 *
 * @param type     tipo da mensagem (ver {@link Types})
 * @param id       identificador único gerado pelo servidor
 * @param replyTo  {@code id} da mensagem do cliente que originou esta resposta, se houver
 * @param roomId   sala a que a mensagem se refere, se houver
 * @param payload  conteúdo, cujo formato depende do tipo
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServerMessage(String type, String id, String replyTo, String roomId, Object payload) {

    public static final class Types {
        public static final String CONNECTED = "CONNECTED";
        public static final String AUTHENTICATED = "AUTHENTICATED";
        public static final String ROOM_STATE = "ROOM_STATE";
        public static final String COMMAND_RESULT = "COMMAND_RESULT";
        public static final String LEFT_ROOM = "LEFT_ROOM";
        public static final String PONG = "PONG";
        public static final String ERROR = "ERROR";

        private Types() {
        }
    }

    public static ServerMessage of(String type, String replyTo, String roomId, Object payload) {
        return new ServerMessage(type, UUID.randomUUID().toString(), replyTo, roomId, payload);
    }
}
