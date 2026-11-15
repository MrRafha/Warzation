package com.warzation.realtime;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
class RulesControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesTheApprovedRulesWithTimesInSeconds() throws Exception {
        mockMvc.perform(get("/api/v1/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provisional", equalTo(true)))
                .andExpect(jsonPath("$.speed", equalTo(1)))
                .andExpect(jsonPath("$.economy.cycleSeconds", equalTo(3600)))
                .andExpect(jsonPath("$.economy.foodDemandPerProvince", equalTo(2)))
                .andExpect(jsonPath("$.economy.baseFoodCapacity", equalTo(200)))
                .andExpect(jsonPath("$.economy.cancelRefundPercent", equalTo(50)))
                .andExpect(jsonPath("$.buildings", hasSize(6)))
                .andExpect(jsonPath("$.buildings[?(@.id == 'FAZENDA')].buildSeconds", contains(7200)))
                .andExpect(jsonPath("$.buildings[?(@.id == 'FAZENDA')].cost.PEDRA", contains(20)))
                .andExpect(jsonPath("$.buildings[?(@.id == 'CELEIRO')].foodCapacityBonus", contains(300)))
                .andExpect(jsonPath("$.buildings[?(@.id == 'MINA')].extraction.resources[0]", contains("PEDRA")))
                .andExpect(jsonPath("$.buildings[?(@.id == 'BIBLIOTECA')].buildSeconds", contains(21600)));
    }
}
