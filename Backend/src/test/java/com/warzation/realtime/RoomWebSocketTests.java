package com.warzation.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warzation.domain.Room;
import com.warzation.domain.RoomRepository;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Prova da issue #9: dois clientes reais conectados ao servidor recebem a mesma
 * atualização, mensagens inválidas são rejeitadas e o estado continua no backend.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RoomWebSocketTests {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    private final List<TestClient> clients = new ArrayList<>();
    private String roomId;

    @BeforeEach
    void createRoom() {
        roomId = "ws-" + UUID.randomUUID().toString().substring(0, 8);
        roomRepository.save(new Room(roomId));
    }

    @AfterEach
    void closeClients() throws Exception {
        for (TestClient client : clients) {
            client.close();
        }
    }

    @Test
    void twoClientsInTheSameRoomReceiveTheSameUpdate() throws Exception {
        TestClient alice = connectAs(TOKEN_1);
        TestClient bob = connectAs(TOKEN_2);

        alice.send("JOIN_ROOM", "join-a", Map.of("roomId", roomId));
        bob.send("JOIN_ROOM", "join-b", Map.of("roomId", roomId));
        assertThat(alice.next("ROOM_STATE").path("payload").path("counter").asLong()).isZero();
        assertThat(bob.next("ROOM_STATE").path("replyTo").asText()).isEqualTo("join-b");

        String commandId = UUID.randomUUID().toString();
        alice.send("COMMAND", "cmd-1", RoomControllerTests.command(commandId, "INCREMENT_COUNTER", 2));

        JsonNode result = alice.next("COMMAND_RESULT");
        assertThat(result.path("replyTo").asText()).isEqualTo("cmd-1");
        assertThat(result.path("payload").path("status").asText()).isEqualTo("APPLIED");
        assertThat(result.path("payload").path("playerId").asText()).isEqualTo("jogador-1");

        JsonNode aliceUpdate = alice.next("ROOM_STATE");
        JsonNode bobUpdate = bob.next("ROOM_STATE");
        assertThat(aliceUpdate.path("payload").path("counter").asLong()).isEqualTo(2);
        assertThat(bobUpdate.path("payload")).isEqualTo(aliceUpdate.path("payload"));
        assertThat(bobUpdate.path("roomId").asText()).isEqualTo(roomId);
    }

    @Test
    void duplicateCommandOverWebSocketIsNotReapplied() throws Exception {
        TestClient alice = connect();
        alice.send("JOIN_ROOM", "join", Map.of("roomId", roomId));
        alice.next("ROOM_STATE");

        Map<String, Object> command = RoomControllerTests.command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 1);
        alice.send("COMMAND", "first", command);
        assertThat(alice.next("COMMAND_RESULT").path("payload").path("status").asText()).isEqualTo("APPLIED");
        alice.send("COMMAND", "again", command);
        JsonNode duplicate = alice.next("COMMAND_RESULT");
        assertThat(duplicate.path("payload").path("status").asText()).isEqualTo("DUPLICATE");
        assertThat(duplicate.path("payload").path("room").path("counter").asLong()).isEqualTo(1);

        assertThat(roomRepository.findById(roomId).orElseThrow().getCounterValue()).isEqualTo(1);
    }

    @Test
    void invalidMessagesAreRejectedAndDoNotChangeState() throws Exception {
        TestClient alice = connect();

        alice.sendRaw("{ isto não é json");
        assertThat(alice.next("ERROR").path("payload").path("code").asText()).isEqualTo("INVALID_MESSAGE");

        alice.send("FAZER_QUALQUER_COISA", "x", Map.of());
        assertThat(alice.next("ERROR").path("payload").path("code").asText()).isEqualTo("UNKNOWN_TYPE");

        alice.send("COMMAND", "sem-sala", RoomControllerTests.command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 1));
        assertThat(alice.next("ERROR").path("payload").path("code").asText()).isEqualTo("NOT_IN_ROOM");

        alice.send("JOIN_ROOM", "join-x", Map.of("roomId", "sala-inexistente"));
        assertThat(alice.next("ERROR").path("payload").path("code").asText()).isEqualTo("ROOM_NOT_FOUND");

        alice.send("JOIN_ROOM", "join", Map.of("roomId", roomId));
        alice.next("ROOM_STATE");
        alice.send("COMMAND", "ruim", RoomControllerTests.command("nao-e-uuid", "INCREMENT_COUNTER", 1));
        JsonNode error = alice.next("ERROR");
        assertThat(error.path("replyTo").asText()).isEqualTo("ruim");
        assertThat(error.path("payload").path("code").asText()).isEqualTo("INVALID_COMMAND");
        assertThat(error.path("payload").path("fields").has("commandId")).isTrue();

        assertThat(roomRepository.findById(roomId).orElseThrow().getCounterValue()).isZero();
    }

    @Test
    void reconnectingClientReceivesTheOfficialStateFromTheServer() throws Exception {
        TestClient alice = connect();
        alice.send("JOIN_ROOM", "join", Map.of("roomId", roomId));
        alice.next("ROOM_STATE");
        alice.send("COMMAND", "cmd", RoomControllerTests.command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 7));
        alice.next("COMMAND_RESULT");
        alice.close();

        TestClient aliceAgain = connect();
        aliceAgain.send("JOIN_ROOM", "rejoin", Map.of("roomId", roomId));
        JsonNode state = aliceAgain.next("ROOM_STATE");
        assertThat(state.path("payload").path("counter").asLong()).isEqualTo(7);
        assertThat(state.path("payload").path("version").asLong()).isEqualTo(1);
    }

    @Test
    void unauthenticatedSessionCannotJoinOrSendCommands() throws Exception {
        TestClient anonymous = connect(ALLOWED_ORIGIN);

        anonymous.send("JOIN_ROOM", "join", Map.of("roomId", roomId));
        assertThat(anonymous.next("ERROR").path("payload").path("code").asText()).isEqualTo("UNAUTHENTICATED");
        anonymous.send("COMMAND", "cmd",
                RoomControllerTests.command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 1));
        assertThat(anonymous.next("ERROR").path("payload").path("code").asText()).isEqualTo("UNAUTHENTICATED");

        anonymous.send("AUTHENTICATE", "auth", Map.of("token", "token-que-nao-existe-123"));
        assertThat(anonymous.next("ERROR").path("payload").path("code").asText()).isEqualTo("UNAUTHENTICATED");

        assertThat(roomRepository.findById(roomId).orElseThrow().getCounterValue()).isZero();
    }

    @Test
    void identityInTheMessageBodyIsIgnored() throws Exception {
        TestClient alice = connectAs(TOKEN_1);
        alice.send("JOIN_ROOM", "join", Map.of("roomId", roomId));
        alice.next("ROOM_STATE");

        Map<String, Object> command = RoomControllerTests.command(UUID.randomUUID().toString(), "INCREMENT_COUNTER", 1);
        command.put("playerId", "jogador-2");
        alice.send("COMMAND", "cmd", command);

        assertThat(alice.next("COMMAND_RESULT").path("payload").path("playerId").asText()).isEqualTo("jogador-1");
    }

    @Test
    void connectionCannotSwitchToAnotherPlayer() throws Exception {
        TestClient alice = connectAs(TOKEN_1);

        alice.send("AUTHENTICATE", "switch", Map.of("token", TOKEN_2));

        assertThat(alice.next("ERROR").path("payload").path("code").asText()).isEqualTo("ALREADY_AUTHENTICATED");
    }

    @Test
    void repeatedInvalidTokensCloseTheConnection() throws Exception {
        TestClient intruder = connect(ALLOWED_ORIGIN);

        for (int attempt = 1; attempt <= 3; attempt++) {
            intruder.send("AUTHENTICATE", "a" + attempt, Map.of("token", "token-errado-" + attempt + "-xxxxxxxx"));
            intruder.next("ERROR");
        }

        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (intruder.session.isOpen() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertThat(intruder.session.isOpen()).isFalse();
    }

    @Test
    void pingIsAnswered() throws Exception {
        TestClient alice = connect();
        alice.send("PING", "p1", null);
        assertThat(alice.next("PONG").path("replyTo").asText()).isEqualTo("p1");
    }

    @Test
    void handshakeFromUnknownOriginIsRefused() {
        assertThatThrownBy(() -> connect("http://site-malicioso.example"))
                .isInstanceOf(ExecutionException.class);
    }

    private static final String TOKEN_1 = "token-de-teste-jogador-1";
    private static final String TOKEN_2 = "token-de-teste-jogador-2";

    /** Conecta e se identifica como jogador 1. */
    private TestClient connect() throws Exception {
        return connectAs(TOKEN_1);
    }

    private TestClient connectAs(String token) throws Exception {
        TestClient client = connect(ALLOWED_ORIGIN);
        client.send("AUTHENTICATE", "auth", Map.of("token", token));
        assertThat(client.next("AUTHENTICATED").path("replyTo").asText()).isEqualTo("auth");
        return client;
    }

    private TestClient connect(String origin) throws Exception {
        TestClient client = new TestClient(objectMapper);
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setOrigin(origin);
        client.session = new StandardWebSocketClient()
                .execute(client, headers, URI.create("ws://localhost:" + port + RealtimeConfiguration.ENDPOINT))
                .get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        clients.add(client);
        assertThat(client.next("CONNECTED").path("payload").path("sessionId").asText()).isNotBlank();
        return client;
    }

    private static final class TestClient extends TextWebSocketHandler {

        private final ObjectMapper objectMapper;
        private final List<JsonNode> received = new ArrayList<>();
        private WebSocketSession session;

        private TestClient(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
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
            sendRaw(objectMapper.writeValueAsString(envelope));
        }

        void sendRaw(String text) throws Exception {
            session.sendMessage(new TextMessage(text));
        }

        /**
         * Remove e devolve a primeira mensagem do tipo pedido, esperando até o
         * timeout. Mensagens de outros tipos continuam na fila, na ordem.
         */
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
                        throw new AssertionError("Nenhuma mensagem " + type + " recebida em " + TIMEOUT
                                + "; recebidas: " + received);
                    }
                    received.wait(remainingMs);
                }
            }
        }

        void close() throws Exception {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
