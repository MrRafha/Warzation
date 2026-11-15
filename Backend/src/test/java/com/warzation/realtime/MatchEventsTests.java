package com.warzation.realtime;

import static com.warzation.realtime.MatchControllerTests.PLAYER_1;
import static com.warzation.realtime.MatchControllerTests.PLAYER_2;
import static com.warzation.realtime.MatchControllerTests.PLAYER_3;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatchEventsTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String matchId;

    @BeforeEach
    void createMatch() throws Exception {
        String body = post(PLAYER_1, "/api/v1/matches", Map.of("commandId", uuid(), "scenarioId", "nordeste-teste",
                "name", "Com histórico")).andReturn().getResponse().getContentAsString();
        matchId = objectMapper.readTree(body).path("match").path("id").asText();
    }

    @Test
    void everyStateChangeIsRecordedOnceAndInOrder() throws Exception {
        post(PLAYER_1, participants(), Map.of("commandId", uuid(), "nationId", "parnaiba"));
        post(PLAYER_2, participants(), Map.of("commandId", uuid(), "nationId", "sao-francisco"));
        post(PLAYER_1, "/api/v1/matches/" + matchId + "/start", Map.of("commandId", uuid()));

        events(PLAYER_2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type",
                        contains("MATCH_CREATED", "PLAYER_JOINED", "PLAYER_JOINED", "MATCH_STARTED")))
                .andExpect(jsonPath("$[*].seq", contains(1, 2, 3, 4)))
                .andExpect(jsonPath("$[0].playerId", equalTo("jogador-1")))
                .andExpect(jsonPath("$[0].data.name", equalTo("Com histórico")))
                .andExpect(jsonPath("$[2].playerName", equalTo("Jogador 2")))
                .andExpect(jsonPath("$[2].data.nationId", equalTo("sao-francisco")))
                .andExpect(jsonPath("$[3].data.neutralProvinces", equalTo(18)))
                .andExpect(content().string(not(containsString("token"))));
    }

    @Test
    void resentAndRejectedCommandsDoNotCreateEvents() throws Exception {
        String join = uuid();
        post(PLAYER_1, participants(), Map.of("commandId", join, "nationId", "parnaiba"));
        post(PLAYER_1, participants(), Map.of("commandId", join, "nationId", "parnaiba"))
                .andExpect(jsonPath("$.status", equalTo("DUPLICATE")));
        post(PLAYER_2, participants(), Map.of("commandId", uuid(), "nationId", "parnaiba"))
                .andExpect(status().isConflict());
        post(PLAYER_1, "/api/v1/matches/" + matchId + "/start", Map.of("commandId", uuid()))
                .andExpect(status().isConflict());

        events(PLAYER_1).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void historyIsForParticipantsAndTheCreator() throws Exception {
        events(PLAYER_1).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        events(PLAYER_3)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo("NOT_PARTICIPANT")));
        mockMvc.perform(get("/api/v1/matches/{id}/events", matchId)).andExpect(status().isUnauthorized());
    }

    private String participants() {
        return "/api/v1/matches/" + matchId + "/participants";
    }

    private ResultActions events(String authorization) throws Exception {
        return mockMvc.perform(get("/api/v1/matches/{id}/events", matchId).header("Authorization", authorization));
    }

    private ResultActions post(String authorization, String path, Map<String, Object> body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(path)
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}
