package com.warzation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.warzation.WarzationApplication;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Prova da issue #11: o estado gravado sobrevive ao encerramento completo da
 * aplicação. Cada fase sobe um contexto Spring novo (outro processo lógico, sem
 * memória compartilhada) apontando para o mesmo banco em arquivo.
 */
class PersistenceAfterRestartTests {

    @TempDir
    Path databaseDirectory;

    @Test
    void stateSurvivesRestartAndRepeatedCommandsAreNotReapplied() {
        String jdbcUrl = "jdbc:h2:file:" + databaseDirectory.resolve("warzation").toAbsolutePath()
                + ";MODE=PostgreSQL";
        RoomCommand command = new RoomCommand(UUID.randomUUID(), RoomCommandType.INCREMENT_COUNTER, 3);

        CommandResult applied;
        try (ConfigurableApplicationContext firstRun = start(jdbcUrl)) {
            RoomCommandService service = firstRun.getBean(RoomCommandService.class);
            applied = service.execute("dev", command);
            assertThat(applied.status()).isEqualTo(CommandResult.Status.APPLIED);
            assertThat(service.getState("dev").counter()).isEqualTo(3);
        }

        try (ConfigurableApplicationContext secondRun = start(jdbcUrl)) {
            RoomCommandService service = secondRun.getBean(RoomCommandService.class);

            RoomState recovered = service.getState("dev");
            assertThat(recovered.counter()).isEqualTo(3);
            assertThat(recovered.version()).isEqualTo(1);
            assertThat(recovered.updatedAt()).isNotNull();

            CommandResult stored = service.findResult("dev", command.commandId());
            assertThat(stored.resultingCounter()).isEqualTo(3);
            assertThat(stored.processedAt()).isNotNull();

            CommandResult resent = service.execute("dev", command);
            assertThat(resent.status()).isEqualTo(CommandResult.Status.DUPLICATE);
            assertThat(service.getState("dev").counter()).isEqualTo(3);
        }

        try (ConfigurableApplicationContext thirdRun = start(jdbcUrl)) {
            assertThat(thirdRun.getBean(RoomCommandService.class).getState("dev").counter()).isEqualTo(3);
        }
    }

    private static ConfigurableApplicationContext start(String jdbcUrl) {
        return new SpringApplicationBuilder(WarzationApplication.class)
                .profiles("test")
                // Argumentos de linha de comando têm precedência sobre application-test.yml.
                .run("--spring.datasource.url=" + jdbcUrl, "--server.port=0", "--spring.main.banner-mode=off");
    }
}
