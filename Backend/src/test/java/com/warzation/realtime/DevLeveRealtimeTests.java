package com.warzation.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/** Com inicialização preguiçosa, os ouvintes de eventos ainda difundem o estado da partida. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:dev-leve-realtime;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test", "dev-leve"})
class DevLeveRealtimeTests {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void matchStateIsBroadcastUnderTheLightProfile() throws Exception {
        String matchId = post("token-de-teste-jogador-1", "/api/v1/matches",
                Map.of("commandId", uuid(), "scenarioId", "nordeste-teste")).path("match").path("id").asText();
        post("token-de-teste-jogador-1", "/api/v1/matches/" + matchId + "/participants",
                Map.of("commandId", uuid(), "nationId", "parnaiba"));

        try (WsTestClient one = WsTestClient.connect(objectMapper, port, "token-de-teste-jogador-1")) {
            one.send("JOIN_MATCH", "j", Map.of("matchId", matchId));
            assertThat(one.next("MATCH_STATE").path("payload").path("match").path("participants")).hasSize(1);

            post("token-de-teste-jogador-2", "/api/v1/matches/" + matchId + "/participants",
                    Map.of("commandId", uuid(), "nationId", "sao-francisco"));

            assertThat(one.next("MATCH_STATE").path("payload").path("match").path("participants")).hasSize(2);
        }
    }

    private JsonNode post(String token, String path, Map<String, Object> body) throws Exception {
        HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isBetween(200, 299);
        return objectMapper.readTree(response.body());
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}
