package com.warzation.application.match;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warzation.domain.match.MatchEvent;
import com.warzation.domain.match.MatchEventRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava eventos na transação de quem chamou: se a mudança de estado for
 * desfeita, o evento também é. A partida deve estar bloqueada (FOR UPDATE) ou
 * recém-criada, o que torna a sequência segura.
 */
@Component
public class MatchEventLog {

    private final MatchEventRepository events;
    private final ObjectMapper objectMapper;

    public MatchEventLog(MatchEventRepository events, ObjectMapper objectMapper) {
        this.events = events;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String matchId, String type, String playerId, UUID commandId, Map<String, Object> data) {
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Dados de evento não serializáveis", exception);
        }
        events.save(new MatchEvent(matchId, events.lastSeq(matchId) + 1, type, playerId, commandId, json));
    }
}
