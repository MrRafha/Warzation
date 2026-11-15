package com.warzation.realtime;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MapControllerTests {

    private static final String BASE = "/api/v1/scenarios";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsAvailableScenarios() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem("nordeste-teste")))
                .andExpect(jsonPath("$[?(@.id == 'nordeste-teste')].provinces", contains(24)))
                .andExpect(jsonPath("$[?(@.id == 'nordeste-teste')].connections", contains(53)));
    }

    @Test
    void returnsTheWholeScenarioWithGeometryAndBuildingSlots() throws Exception {
        mockMvc.perform(get(BASE + "/nordeste-teste"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.scenario.id", equalTo("nordeste-teste")))
                .andExpect(jsonPath("$.scenario.provinces", hasSize(24)))
                .andExpect(jsonPath("$.scenario.connections", hasSize(53)))
                .andExpect(jsonPath("$.scenario.nations", hasSize(2)))
                .andExpect(jsonPath("$.geometry.features", hasSize(24)))
                .andExpect(jsonPath("$.buildingSlots['pi-teresina']", equalTo(4)))
                .andExpect(jsonPath("$.buildingSlots['pe-recife']", equalTo(3)));
    }

    @Test
    void everyClientGetsTheSameMap() throws Exception {
        MvcResult first = mockMvc.perform(get(BASE + "/nordeste-teste")).andReturn();
        MvcResult second = mockMvc.perform(get(BASE + "/nordeste-teste")).andReturn();
        String etag = first.getResponse().getHeader("ETag");

        org.assertj.core.api.Assertions.assertThat(second.getResponse().getContentAsString())
                .isEqualTo(first.getResponse().getContentAsString());
        mockMvc.perform(get(BASE + "/nordeste-teste").header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void neighboursMatchTheRegisteredConnections() throws Exception {
        mockMvc.perform(get(BASE + "/nordeste-teste/provinces/pi-teresina/neighbours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].id", hasItem("pi-floriano")))
                .andExpect(jsonPath("$[*].id", hasItem("ma-caxias")))
                .andExpect(jsonPath("$[?(@.id == 'ce-sobral')].state", contains("CE")));
    }

    @Test
    void answersWhetherTwoProvincesShareABorder() throws Exception {
        mockMvc.perform(get(BASE + "/nordeste-teste/adjacency").param("from", "pi-teresina").param("to", "pi-floriano"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adjacent", equalTo(true)));
        mockMvc.perform(get(BASE + "/nordeste-teste/adjacency").param("from", "pi-teresina").param("to", "ba-salvador"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adjacent", equalTo(false)));
    }

    @Test
    void computesTheShortestRoute() throws Exception {
        mockMvc.perform(get(BASE + "/nordeste-teste/routes").param("from", "pi-teresina").param("to", "pi-floriano"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provinces", contains("pi-teresina", "pi-floriano")))
                .andExpect(jsonPath("$.borders", equalTo(1)));
        mockMvc.perform(get(BASE + "/nordeste-teste/routes").param("from", "pi-teresina").param("to", "ba-salvador"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provinces[0]", equalTo("pi-teresina")))
                .andExpect(jsonPath("$.provinces[-1]", equalTo("ba-salvador")));
    }

    @Test
    void unknownProvinceOrScenarioReturnsNotFound() throws Exception {
        mockMvc.perform(get(BASE + "/nordeste-teste/provinces/pi-atlantida/neighbours"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo("NOT_FOUND")))
                .andExpect(jsonPath("$.message", equalTo("Província não encontrada: pi-atlantida")));
        mockMvc.perform(get(BASE + "/nordeste-teste/routes").param("from", "pi-teresina").param("to", "xx"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/cenario-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
    }

    @Test
    void missingParametersAreABadRequest() throws Exception {
        mockMvc.perform(get(BASE + "/nordeste-teste/routes").param("from", "pi-teresina"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo("BAD_REQUEST")));
    }
}
