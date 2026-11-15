package com.warzation.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Cliente WebSocket de teste que guarda as mensagens recebidas e espera por tipo. */
final class WsTestClient extends TextWebSocketHandler implements AutoCloseable {

    static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final ObjectMapper objectMapper;
    private final List<JsonNode> received = new ArrayList<>();
    private WebSocketSession session;

    private WsTestClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    static WsTestClient connect(ObjectMapper objectMapper, int port, String token) throws Exception {
        WsTestClient client = new WsTestClient(objectMapper);
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setOrigin("http://localhost:5173");
        client.session = new StandardWebSocketClient()
                .execute(client, headers, URI.create("ws://localhost:" + port + RealtimeConfiguration.ENDPOINT))
                .get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        client.next("CONNECTED");
        client.send("AUTHENTICATE", "auth", Map.of("token", token));
        client.next("AUTHENTICATED");
        return client;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode parsed = objectMapper.readTree(message.getPayload());
        synchronized (received) {
            received.add(parsed);
            received.notifyAll();
        }
    }

    void send(String type, String id, Object payload) throws Exception {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", type);
        envelope.put("id", id);
        if (payload != null) {
            envelope.put("payload", payload);
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(envelope)));
    }

    /** Remove e devolve a primeira mensagem do tipo pedido; as outras continuam na fila. */
    JsonNode next(String type) throws InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        synchronized (received) {
            while (true) {
                for (Iterator<JsonNode> it = received.iterator(); it.hasNext(); ) {
                    JsonNode message = it.next();
                    if (type.equals(message.path("type").asText())) {
                        it.remove();
                        return message;
                    }
                }
                long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                if (remainingMs <= 0) {
                    throw new AssertionError("Nenhuma mensagem " + type + " em " + TIMEOUT + "; recebidas: " + received);
                }
                received.wait(remainingMs);
            }
        }
    }

    /** Verdadeiro se chegar alguma mensagem do tipo dentro do prazo curto. */
    boolean receives(String type, Duration within) throws InterruptedException {
        long deadline = System.nanoTime() + within.toNanos();
        synchronized (received) {
            while (true) {
                if (received.stream().anyMatch(m -> type.equals(m.path("type").asText()))) {
                    return true;
                }
                long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                if (remainingMs <= 0) {
                    return false;
                }
                received.wait(remainingMs);
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
}
