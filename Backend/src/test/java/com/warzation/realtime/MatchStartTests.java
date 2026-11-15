package com.warzation.realtime;

import static com.warzation.realtime.MatchControllerTests.PLAYER_1;
import static com.warzation.realtime.MatchControllerTests.PLAYER_2;
import static com.warzation.realtime.MatchControllerTests.PLAYER_3;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatchStartTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String matchId;

    @BeforeEach
    void createMatch() throws Exception {
        String body = post(PLAYER_1, "/api/v1/matches", Map.of("commandId", uuid(), "scenarioId", "nordeste-teste"))
                .andReturn().getResponse().getContentAsString();
        matchId = objectMapper.readTree(body).path("match").path("id").asText();
    }

    @Test
    void startingAppliesTheInitialPosition() throws Exception {
        joinBoth();

        post(PLAYER_1, "/api/v1/matches/" + matchId + "/start", Map.of("commandId", uuid()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.match.status", equalTo("STARTED")))
                .andExpect(jsonPath("$.match.startedAt", notNullValue()));

        mockMvc.perform(get("/api/v1/matches/{id}/state", matchId).header("Authorization", PLAYER_2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("STARTED")))
                .andExpect(jsonPath("$.provinces", hasSize(24)))
                .andExpect(jsonPath("$.provinces[?(@.provinceId == 'pi-teresina')].ownerNationId", contains("parnaiba")))
                .andExpect(jsonPath("$.provinces[?(@.provinceId == 'pi-teresina')].ownerPlayerId", contains("jogador-1")))
                .andExpect(jsonPath("$.provinces[?(@.provinceId == 'ba-salvador')].ownerPlayerId", contains("jogador-2")))
                .andExpect(jsonPath("$.provinces[?(@.provinceId == 'pi-teresina')].units[0].quantity", contains(2)))
                .andExpect(jsonPath("$.provinces[?(@.provinceId == 'pi-teresina')].stock.COMIDA", contains(60)))
                .andExpect(jsonPath("$.provinces[?(@.provinceId == 'pe-recife')].ownerNationId", contains(nullValue())))
                .andExpect(jsonPath("$.provinces[?(@.provinceId == 'pe-recife')].units[0].quantity", contains(1)))
                .andExpect(jsonPath("$.provinces[?(@.ownerNationId == 'parnaiba')]", hasSize(3)))
                .andExpect(jsonPath("$.provinces[?(@.ownerNationId == 'sao-francisco')]", hasSize(3)));
    }

    @Test
    void onlyTheCreatorStartsAndWithEnoughPlayers() throws Exception {
        post(PLAYER_1, "/api/v1/matches/" + matchId + "/participants", Map.of("commandId", uuid(), "nationId", "parnaiba"));

        post(PLAYER_1, "/api/v1/matches/" + matchId + "/start", Map.of("commandId", uuid()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("NOT_ENOUGH_PLAYERS")));

        post(PLAYER_2, "/api/v1/matches/" + matchId + "/participants",
                Map.of("commandId", uuid(), "nationId", "sao-francisco"));
        post(PLAYER_2, "/api/v1/matches/" + matchId + "/start", Map.of("commandId", uuid()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo("NOT_MATCH_CREATOR")));
    }

    @Test
    void startedMatchRejectsNewPlayersAndASecondStart() throws Exception {
        joinBoth();
        String startCommand = uuid();
        post(PLAYER_1, "/api/v1/matches/" + matchId + "/start", Map.of("commandId", startCommand))
                .andExpect(status().isCreated());

        post(PLAYER_1, "/api/v1/matches/" + matchId + "/start", Map.of("commandId", startCommand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("DUPLICATE")));
        post(PLAYER_1, "/api/v1/matches/" + matchId + "/start", Map.of("commandId", uuid()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("MATCH_NOT_OPEN")));
        post(PLAYER_3, "/api/v1/matches/" + matchId + "/participants", Map.of("commandId", uuid(), "nationId", "parnaiba"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("MATCH_NOT_OPEN")));

        mockMvc.perform(get("/api/v1/matches/{id}/state", matchId).header("Authorization", PLAYER_1))
                .andExpect(jsonPath("$.provinces", hasSize(24)))
                .andExpect(jsonPath("$.provinces[*].units", everyItem(hasSize(1))));
    }

    @Test
    void stateIsOnlyForParticipants() throws Exception {
        joinBoth();

        mockMvc.perform(get("/api/v1/matches/{id}/state", matchId).header("Authorization", PLAYER_3))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo("NOT_PARTICIPANT")));
        mockMvc.perform(get("/api/v1/matches/{id}/state", matchId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/matches/{id}/state", matchId).header("Authorization", PLAYER_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("OPEN")))
                .andExpect(jsonPath("$.provinces", hasSize(0)));
    }

    private void joinBoth() throws Exception {
        post(PLAYER_1, "/api/v1/matches/" + matchId + "/participants", Map.of("commandId", uuid(), "nationId", "parnaiba"))
                .andExpect(status().isCreated());
        post(PLAYER_2, "/api/v1/matches/" + matchId + "/participants",
                Map.of("commandId", uuid(), "nationId", "sao-francisco"))
                .andExpect(status().isCreated());
    }

    private ResultActions post(String authorization, String path, Map<String, Object> body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}
