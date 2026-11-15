package com.warzation.application.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.warzation.WarzationApplication;
import com.warzation.config.GameRulesValidationFailureAnalyzer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.ConfigurableApplicationContext;

/** As regras são carregadas no startup; dados inválidos impedem a subida com mensagem clara. */
class GameRulesStartupTests {

    @TempDir
    Path dir;

    @Test
    void invalidRulesStopTheApplication() throws Exception {
        String valid = Files.readString(Path.of("data/rules/game-rules.json"));
        Path rules = dir.resolve("game-rules.json");
        Files.writeString(rules, valid.replace("\"buildMinutes\": 120", "\"buildMinutes\": 2000"));

        FailureAnalysis analysis = failureFor("--application.rules.location=file:" + rules.toAbsolutePath());

        assertThat(analysis.getDescription()).contains("game-rules.json").contains("FAZENDA").contains("24 h");
        assertThat(analysis.getAction()).contains("Backend/data/rules/game-rules.json");
    }

    @Test
    void unknownFieldIsReportedWithItsPath() throws Exception {
        String valid = Files.readString(Path.of("data/rules/game-rules.json"));
        Path rules = dir.resolve("game-rules.json");
        Files.writeString(rules, valid.replace("\"cycleMinutes\": 60", "\"cycleMinutes\": 60, \"ciclo\": 1"));

        FailureAnalysis analysis = failureFor("--application.rules.location=file:" + rules.toAbsolutePath());

        assertThat(analysis.getDescription()).contains("ciclo").contains("$.economy");
    }

    @Test
    void missingFileStopsTheApplication() {
        FailureAnalysis analysis = failureFor("--application.rules.location=file:" + dir.resolve("nada.json"));
        assertThat(analysis.getDescription()).contains("arquivo não encontrado");
    }

    @Test
    void speedOutsideTheAllowedRangeStopsTheApplication() {
        FailureAnalysis analysis = failureFor("--application.rules.speed=0");
        assertThat(analysis.getDescription()).contains("application.rules.speed");
    }

    @Test
    void speedScalesCycleAndBuildDurations() {
        try (ConfigurableApplicationContext context = run("speed-ok", "--application.rules.speed=60")) {
            GameRules rules = context.getBean(GameRules.class);
            assertThat(rules.cycleDuration()).isEqualTo(Duration.ofMinutes(1));
            assertThat(rules.buildDuration(rules.building("FAZENDA"))).isEqualTo(Duration.ofMinutes(2));
            assertThat(rules.buildDuration(rules.building("BIBLIOTECA"))).isEqualTo(Duration.ofMinutes(6));
        }
    }

    private FailureAnalysis failureFor(String... args) {
        Throwable failure = catchThrowable(() -> run("invalid-rules", args).close());
        assertThat(failure).isNotNull();
        FailureAnalysis analysis = new GameRulesValidationFailureAnalyzer().analyze(failure);
        assertThat(analysis).as(String.valueOf(failure)).isNotNull();
        return analysis;
    }

    private static ConfigurableApplicationContext run(String db, String... args) {
        String[] all = new String[args.length + 3];
        System.arraycopy(args, 0, all, 0, args.length);
        all[args.length] = "--spring.datasource.url=jdbc:h2:mem:" + db + ";MODE=PostgreSQL";
        all[args.length + 1] = "--server.port=0";
        all[args.length + 2] = "--spring.main.banner-mode=off";
        return new SpringApplicationBuilder(WarzationApplication.class).profiles("test").run(all);
    }
}
