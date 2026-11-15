package com.warzation.realtime;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlayerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void identifiesThePlayerOfTheToken() throws Exception {
        mockMvc.perform(get("/api/v1/players/me").header("Authorization", "Bearer token-de-teste-jogador-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo("jogador-2")))
                .andExpect(jsonPath("$.name", equalTo("Jogador 2")))
                .andExpect(jsonPath("$.token").doesNotExist());
        mockMvc.perform(get("/api/v1/players/me").header("Authorization", "bearer token-de-teste-jogador-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo("jogador-1")));
    }

    @Test
    void missingOrInvalidTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/players/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo("UNAUTHENTICATED")));
        mockMvc.perform(get("/api/v1/players/me").header("Authorization", "Bearer outro-token-qualquer"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicReadsDoNotNeedIdentity() throws Exception {
        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/scenarios/nordeste-teste")).andExpect(status().isOk());
    }
}
