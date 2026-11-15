package com.warzation.realtime;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warzation.domain.match.MatchRepository;
import com.warzation.domain.match.MatchStatus;
import java.util.LinkedHashMap;
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
class MatchControllerTests {

    static final String PLAYER_1 = "Bearer token-de-teste-jogador-1";
    static final String PLAYER_2 = "Bearer token-de-teste-jogador-2";
    static final String PLAYER_3 = "Bearer token-de-teste-jogador-3";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MatchRepository matchRepository;

    private String matchId;

    @BeforeEach
    void createMatch() throws Exception {
        JsonNode created = json(create(PLAYER_1, uuid(), "nordeste-teste", "Partida de teste")
                .andExpect(status().isCreated()));
        matchId = created.path("match").path("id").asText();
    }

    @Test
    void createdMatchIsListedAndOpen() throws Exception {
        mockMvc.perform(get("/api/v1/matches/{id}", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", equalTo("Partida de teste")))
                .andExpect(jsonPath("$.scenarioId", equalTo("nordeste-teste")))
                .andExpect(jsonPath("$.status", equalTo("OPEN")))
                .andExpect(jsonPath("$.maxPlayers", equalTo(2)))
                .andExpect(jsonPath("$.createdBy", equalTo("jogador-1")))
                .andExpect(jsonPath("$.participants", hasSize(0)))
                .andExpect(jsonPath("$.nations[*].id", hasItem("parnaiba")))
                .andExpect(jsonPath("$.nations[0].playerId", nullValue()));

        mockMvc.perform(get("/api/v1/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(matchId)));
        mockMvc.perform(get("/api/v1/matches").param("status", "OPEN"))
                .andExpect(jsonPath("$[*].id", hasItem(matchId)));
        mockMvc.perform(get("/api/v1/matches").param("status", "FINISHED"))
                .andExpect(jsonPath("$[*].id", not(hasItem(matchId))));
    }

    @Test
    void createsWithDefaultNameAndRejectsUnknownScenario() throws Exception {
        create(PLAYER_2, uuid(), "nordeste-teste", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.match.name", equalTo("Partida de Jogador 2")));
        create(PLAYER_1, uuid(), "cenario-inexistente", null).andExpect(status().isNotFound());
        create(PLAYER_1, uuid(), "nordeste-teste", "x".repeat(61))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    void resendingCreateReturnsTheSameMatch() throws Exception {
        String commandId = uuid();
        String first = json(create(PLAYER_1, commandId, "nordeste-teste", "Uma")).path("match").path("id").asText();

        create(PLAYER_1, commandId, "nordeste-teste", "Uma")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("DUPLICATE")))
                .andExpect(jsonPath("$.match.id", equalTo(first)));
        create(PLAYER_2, commandId, "nordeste-teste", "Uma")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("COMMAND_CONFLICT")));
    }

    @Test
    void twoPlayersJoinWithDifferentNations() throws Exception {
        join(PLAYER_1, uuid(), "parnaiba").andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", equalTo("APPLIED")));
        join(PLAYER_2, uuid(), "sao-francisco").andExpect(status().isCreated())
                .andExpect(jsonPath("$.match.participants", hasSize(2)));

        mockMvc.perform(get("/api/v1/matches/{id}", matchId))
                .andExpect(jsonPath("$.participants[0].playerId", equalTo("jogador-1")))
                .andExpect(jsonPath("$.participants[0].playerName", equalTo("Jogador 1")))
                .andExpect(jsonPath("$.participants[0].nationId", equalTo("parnaiba")))
                .andExpect(jsonPath("$.participants[1].nationId", equalTo("sao-francisco")))
                .andExpect(jsonPath("$.nations[?(@.id == 'parnaiba')].playerId", hasItem("jogador-1")));
    }

    @Test
    void takenNationIsRejected() throws Exception {
        join(PLAYER_1, uuid(), "parnaiba").andExpect(status().isCreated());

        join(PLAYER_2, uuid(), "parnaiba")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("NATION_TAKEN")));
        expectParticipants(1);
    }

    @Test
    void fullMatchIsRejected() throws Exception {
        join(PLAYER_1, uuid(), "parnaiba").andExpect(status().isCreated());
        join(PLAYER_2, uuid(), "sao-francisco").andExpect(status().isCreated());

        join(PLAYER_3, uuid(), "parnaiba")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("MATCH_FULL")));
        expectParticipants(2);
    }

    @Test
    void playerCannotJoinTwice() throws Exception {
        join(PLAYER_1, uuid(), "parnaiba").andExpect(status().isCreated());

        join(PLAYER_1, uuid(), "sao-francisco")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("ALREADY_JOINED")));
        expectParticipants(1);
    }

    @Test
    void resendingJoinDoesNotDuplicateTheParticipant() throws Exception {
        String commandId = uuid();
        join(PLAYER_1, commandId, "parnaiba").andExpect(status().isCreated());

        join(PLAYER_1, commandId, "parnaiba")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("DUPLICATE")))
                .andExpect(jsonPath("$.match.participants", hasSize(1)));
        join(PLAYER_1, commandId, "sao-francisco")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("COMMAND_CONFLICT")));
        expectParticipants(1);
    }

    @Test
    void closedMatchDoesNotAcceptPlayers() throws Exception {
        var match = matchRepository.findById(matchId).orElseThrow();
        match.changeStatus(MatchStatus.FINISHED);
        matchRepository.save(match);

        join(PLAYER_1, uuid(), "parnaiba")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", equalTo("MATCH_NOT_OPEN")));
        expectParticipants(0);
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        join(PLAYER_1, uuid(), "nacao-inexistente")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.nationId").exists());
        join(PLAYER_1, "nao-e-uuid", "parnaiba")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.commandId").exists());
        mockMvc.perform(post("/api/v1/matches/{id}/participants", "partida-inexistente")
                        .header("Authorization", PLAYER_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("commandId", uuid(), "nationId", "parnaiba"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/matches").param("status", "QUALQUER"))
                .andExpect(status().isBadRequest());
        expectParticipants(0);
    }

    @Test
    void joiningRequiresIdentity() throws Exception {
        join(null, uuid(), "parnaiba")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo("UNAUTHENTICATED")));
        create(null, uuid(), "nordeste-teste", null).andExpect(status().isUnauthorized());
        expectParticipants(0);
    }

    private ResultActions create(String authorization, String commandId, String scenarioId, String name)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("commandId", commandId);
        body.put("scenarioId", scenarioId);
        if (name != null) {
            body.put("name", name);
        }
        return perform(post("/api/v1/matches"), authorization, body);
    }

    private ResultActions join(String authorization, String commandId, String nationId) throws Exception {
        return perform(post("/api/v1/matches/{id}/participants", matchId), authorization,
                Map.of("commandId", commandId, "nationId", nationId));
    }

    private ResultActions perform(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String authorization,
            Map<String, Object> body) throws Exception {
        request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return mockMvc.perform(request);
    }

    private void expectParticipants(int count) throws Exception {
        mockMvc.perform(get("/api/v1/matches/{id}", matchId))
                .andExpect(jsonPath("$.participants", hasSize(count)));
    }

    private JsonNode json(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}
