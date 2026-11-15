package com.warzation.application.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.warzation.WarzationApplication;
import com.warzation.config.ScenarioValidationFailureAnalyzer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Dados de cenário inválidos impedem o startup com uma mensagem que aponta o arquivo e o problema. */
class InvalidScenarioStartupTests {

    @TempDir
    Path dir;

    @Test
    void invalidScenarioStopsTheApplication() throws Exception {
        Map<String, Object> scenario = TestScenarios.valid();
        TestScenarios.connections(scenario).add(new ArrayList<>(List.of("p1", "p42")));
        TestScenarios.write(dir, scenario, TestScenarios.geometryFor(8));

        Throwable failure = catchThrowable(() -> new SpringApplicationBuilder(WarzationApplication.class)
                .profiles("test")
                .run("--application.scenarios.location=file:" + dir.toAbsolutePath() + "/*/scenario.json",
                        "--spring.datasource.url=jdbc:h2:mem:invalid-scenario;MODE=PostgreSQL",
                        "--server.port=0", "--spring.main.banner-mode=off"));

        assertThat(failure).isNotNull();
        FailureAnalysis analysis = new ScenarioValidationFailureAnalyzer().analyze(failure);
        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("teste/scenario.json").contains("p42");
        assertThat(analysis.getAction()).contains("validate_scenario.py");
    }
}
