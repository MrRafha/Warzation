package com.warzation.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prova da issue #20: dois clientes reais veem o mesmo estado territorial; uma
 * ordem enviada por um é validada pelo servidor e chega ao outro; quem não
 * participa não recebe nada; reconectar restaura o estado oficial.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MatchRealtimeTests {

    private static final String TOKEN_1 = "token-de-teste-jogador-1";
    private static final String TOKEN_2 = "token-de-teste-jogador-2";
    private static final String TOKEN_3 = "token-de-teste-jogador-3";

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newHttpClient();
    private final List<WsTestClient> clients = new ArrayList<>();
    private String matchId;

    @BeforeEach
    void createMatchWithTwoPlayers() throws Exception {
        matchId = post(TOKEN_1, "/api/v1/matches", Map.of("commandId", uuid(), "scenarioId", "nordeste-teste"))
                .path("match").path("id").asText();
        post(TOKEN_1, "/api/v1/matches/" + matchId + "/participants", Map.of("commandId", uuid(), "nationId", "parnaiba"));
        post(TOKEN_2, "/api/v1/matches/" + matchId + "/participants",
                Map.of("commandId", uuid(), "nationId", "sao-francisco"));
    }

    @AfterEach
    void close() throws Exception {
        for (WsTestClient client : clients) {
            client.close();
        }
    }

    @Test
    void bothPlayersSeeTheSameStateAndAnOrderFromOneReachesTheOther() throws Exception {
        WsTestClient one = connect(TOKEN_1);
        WsTestClient two = connect(TOKEN_2);

        one.send("JOIN_MATCH", "j1", Map.of("matchId", matchId));
        two.send("JOIN_MATCH", "j2", Map.of("matchId", matchId));
        JsonNode first = one.next("MATCH_STATE");
        JsonNode second = two.next("MATCH_STATE");
        assertThat(first.path("replyTo").asText()).isEqualTo("j1");
        assertThat(first.path("payload").path("match").path("status").asText()).isEqualTo("OPEN");
        assertThat(second.path("payload")).isEqualTo(first.path("payload"));

        // ordem enviada pelo jogador 1 (HTTP), validada e aplicada pelo servidor
        post(TOKEN_1, "/api/v1/matches/" + matchId + "/start", Map.of("commandId", uuid()));

        JsonNode afterStartOne = one.next("MATCH_STATE");
        JsonNode afterStartTwo = two.next("MATCH_STATE");
        assertThat(afterStartTwo.path("payload")).isEqualTo(afterStartOne.path("payload"));
        JsonNode state = afterStartTwo.path("payload").path("state");
        assertThat(state.path("status").asText()).isEqualTo("STARTED");
        assertThat(state.path("provinces")).hasSize(24);
        assertThat(ownerOf(state, "pi-teresina")).isEqualTo("parnaiba");
        assertThat(ownerOf(state, "ba-salvador")).isEqualTo("sao-francisco");
    }

    @Test
    void rejectedOrderDoesNotReachAnyone() throws Exception {
        WsTestClient one = connect(TOKEN_1);
        one.send("JOIN_MATCH", "j1", Map.of("matchId", matchId));
        one.next("MATCH_STATE");

        HttpResponse<String> refused = postRaw(TOKEN_2, "/api/v1/matches/" + matchId + "/start", Map.of("commandId", uuid()));

        assertThat(refused.statusCode()).isEqualTo(403);
        assertThat(one.receives("MATCH_STATE", Duration.ofMillis(500))).isFalse();
    }

    @Test
    void nonParticipantDoesNotReceiveTheMatch() throws Exception {
        WsTestClient outsider = connect(TOKEN_3);

        outsider.send("JOIN_MATCH", "j3", Map.of("matchId", matchId));
        JsonNode error = outsider.next("ERROR");
        assertThat(error.path("payload").path("code").asText()).isEqualTo("NOT_PARTICIPANT");

        post(TOKEN_1, "/api/v1/matches/" + matchId + "/start", Map.of("commandId", uuid()));
        assertThat(outsider.receives("MATCH_STATE", Duration.ofMillis(500))).isFalse();

        outsider.send("JOIN_MATCH", "j4", Map.of("matchId", "partida-inexistente"));
        assertThat(outsider.next("ERROR").path("payload").path("code").asText()).isEqualTo("MATCH_NOT_FOUND");
    }

    @Test
    void reconnectingRestoresTheOfficialState() throws Exception {
        post(TOKEN_1, "/api/v1/matches/" + matchId + "/start", Map.of("commandId", uuid()));
        WsTestClient two = connect(TOKEN_2);
        two.send("JOIN_MATCH", "j", Map.of("matchId", matchId));
        JsonNode before = two.next("MATCH_STATE");
        two.close();

        WsTestClient again = connect(TOKEN_2);
        again.send("JOIN_MATCH", "j-again", Map.of("matchId", matchId));
        JsonNode after = again.next("MATCH_STATE");

        assertThat(after.path("payload").path("state")).isEqualTo(before.path("payload").path("state"));
        assertThat(after.path("payload").path("state").path("provinces")).hasSize(24);
    }

    @Test
    void joinMatchRequiresTheMatchId() throws Exception {
        WsTestClient one = connect(TOKEN_1);
        one.send("JOIN_MATCH", "j", Map.of());
        assertThat(one.next("ERROR").path("payload").path("code").asText()).isEqualTo("INVALID_MESSAGE");
    }

    private WsTestClient connect(String token) throws Exception {
        WsTestClient client = WsTestClient.connect(objectMapper, port, token);
        clients.add(client);
        return client;
    }

    private JsonNode post(String token, String path, Map<String, Object> body) throws Exception {
        HttpResponse<String> response = postRaw(token, path, body);
        assertThat(response.statusCode()).as("POST %s: %s", path, response.body()).isBetween(200, 299);
        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> postRaw(String token, String path, Map<String, Object> body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String ownerOf(JsonNode state, String provinceId) {
        for (JsonNode province : state.path("provinces")) {
            if (province.path("provinceId").asText().equals(provinceId)) {
                return province.path("ownerNationId").asText(null);
            }
        }
        return null;
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}
