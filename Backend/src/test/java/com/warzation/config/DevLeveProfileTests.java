package com.warzation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.warzation.WarzationApplication;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * O perfil dev-leve (inicialização preguiçosa, pool pequeno) não pode desligar
 * as validações de startup nem enfraquecer o pool além do previsto.
 */
@SpringBootTest
@ActiveProfiles({"test", "dev-leve"})
class DevLeveProfileTests {

    @Autowired
    private DataSource dataSource;

    @TempDir
    Path dir;

    @Test
    void usesASmallPoolWithoutIdleConnections() {
        HikariDataSource hikari = (HikariDataSource) dataSource;
        assertThat(hikari.getMaximumPoolSize()).isEqualTo(3);
        assertThat(hikari.getMinimumIdle()).isZero();
    }

    @Test
    void invalidScenarioStillStopsStartupWithLazyInitialization() throws Exception {
        Path scenario = Files.createDirectories(dir.resolve("quebrado")).resolve("scenario.json");
        Files.writeString(scenario, "{ \"formatVersion\": 1, ");

        Throwable failure = catchThrowable(() -> {
            try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(WarzationApplication.class)
                    .profiles("test", "dev-leve")
                    .run("--application.scenarios.location=file:" + dir.toAbsolutePath() + "/*/scenario.json",
                            "--spring.datasource.url=jdbc:h2:mem:dev-leve-invalido;MODE=PostgreSQL",
                            "--server.port=0", "--spring.main.banner-mode=off")) {
                // não deveria chegar aqui
            }
        });

        assertThat(failure).isNotNull();
        assertThat(new ScenarioValidationFailureAnalyzer().analyze(failure)).isNotNull();
    }
}
