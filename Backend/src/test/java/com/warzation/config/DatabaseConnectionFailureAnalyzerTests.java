package com.warzation.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

class DatabaseConnectionFailureAnalyzerTests {

    private final DatabaseConnectionFailureAnalyzer analyzer = new DatabaseConnectionFailureAnalyzer();

    @Test
    void explainsConnectionRefused() {
        FailureAnalysis analysis = analyzer.analyze(wrap(new SQLException("Connection refused", "08001")));

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("Não foi possível conectar ao PostgreSQL")
                .contains("não respondeu");
        assertThat(analysis.getAction()).contains("DB_URL").contains("DB_PASSWORD");
    }

    @Test
    void explainsAuthenticationFailure() {
        FailureAnalysis analysis = analyzer.analyze(
                wrap(new SQLException("password authentication failed", "28P01")));

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("usuário ou a senha");
    }

    @Test
    void explainsMissingDatabase() {
        FailureAnalysis analysis = analyzer.analyze(wrap(new SQLException("database does not exist", "3D000")));

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("não existe");
    }

    @Test
    void ignoresUnrelatedSqlErrors() {
        assertThat(analyzer.analyze(wrap(new SQLException("syntax error", "42601")))).isNull();
        assertThat(analyzer.analyze(new IllegalStateException("sem SQL"))).isNull();
    }

    private static Throwable wrap(SQLException cause) {
        return new IllegalStateException("Falha no startup", new RuntimeException("Flyway", cause));
    }
}
