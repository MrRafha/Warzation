package com.warzation.realtime;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
