package com.warzation.config;

import java.sql.SQLException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Troca o stack trace de falha de conexão com o PostgreSQL por uma mensagem
 * curta no startup, com a causa provável e o que conferir.
 */
public class DatabaseConnectionFailureAnalyzer extends AbstractFailureAnalyzer<SQLException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, SQLException cause) {
        String sqlState = cause.getSQLState();
        if (sqlState == null) {
            return null;
        }

        String reason;
        if (sqlState.startsWith("08")) {
            reason = "O servidor PostgreSQL não respondeu no endereço configurado.";
        } else if ("28P01".equals(sqlState) || "28000".equals(sqlState)) {
            reason = "O PostgreSQL recusou o usuário ou a senha configurados.";
        } else if ("3D000".equals(sqlState)) {
            reason = "O banco de dados configurado não existe no PostgreSQL.";
        } else {
            return null;
        }

        String description = "Não foi possível conectar ao PostgreSQL. " + reason
                + System.lineSeparator() + "Detalhe do driver: " + cause.getMessage();
        String action = """
                Confira:
                  1. se o serviço do PostgreSQL está em execução;
                  2. se DB_URL aponta para o host, a porta e o banco corretos;
                  3. se DB_USERNAME e DB_PASSWORD estão definidos (ou application-local.yml foi criado);
                  4. se o banco e o usuário foram criados conforme Docs/04_GUIA_DESENVOLVIMENTO_LOCAL.md.""";
        return new FailureAnalysis(description, action, cause);
    }
}
