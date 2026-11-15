package com.warzation.realtime;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("UP")))
                .andExpect(jsonPath("$.service", equalTo("warzation-backend")));
    }

    @Test
    void invalidMetadataReturnsStructuredValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("key", "", "value", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.status", equalTo(400)))
                .andExpect(jsonPath("$.fields.key").exists())
                .andExpect(jsonPath("$.fields.value").exists());
    }

    @Test
    void metadataCanBeWrittenAndRead() throws Exception {
        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("key", "integration-key", "value", "persisted-value"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value", equalTo("persisted-value")));

        mockMvc.perform(get("/api/v1/metadata/integration-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value", equalTo("persisted-value")));
    }

    @Test
    void unknownRouteReturnsStructuredNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/rota-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo("NOT_FOUND")))
                .andExpect(jsonPath("$.status", equalTo(404)));
    }

    @Test
    void malformedJsonReturnsBadRequestInsteadOfInternalError() throws Exception {
        mockMvc.perform(post("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isto não é json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void corsAllowsConfiguredLocalOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/health")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(get("/api/v1/health").header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(get("/api/v1/health").header("Origin", "http://site-malicioso.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
