package com.warzation.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/** Serializa e envia mensagens do servidor, encerrando sessões que falham. */
@Component
public class RealtimeSender {

    private static final Logger log = LoggerFactory.getLogger(RealtimeSender.class);

    private final ObjectMapper objectMapper;

    public RealtimeSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void send(WebSocketSession session, ServerMessage message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (JsonProcessingException exception) {
            log.error("Falha ao serializar mensagem {}", message.type(), exception);
        } catch (IOException | RuntimeException exception) {
            log.warn("Falha ao enviar {} para a sessão {}; encerrando: {}",
                    message.type(), session.getId(), exception.getMessage());
            try {
                session.close(CloseStatus.SESSION_NOT_RELIABLE);
            } catch (IOException ignored) {
                // a sessão já está inutilizável
            }
        }
    }
}
