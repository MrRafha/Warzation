package com.warzation.config;

import com.warzation.application.rules.GameRulesValidationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Lista, no startup, cada problema encontrado nas regras de jogo. */
public class GameRulesValidationFailureAnalyzer extends AbstractFailureAnalyzer<GameRulesValidationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, GameRulesValidationException cause) {
        StringBuilder description = new StringBuilder("As regras de jogo em ")
                .append(cause.getSource())
                .append(" são inválidas:");
        cause.getProblems().forEach(problem -> description.append(System.lineSeparator()).append("  - ").append(problem));
        String action = "Corrija Backend/data/rules/game-rules.json. O formato está em Backend/data/README.md.";
        return new FailureAnalysis(description.toString(), action, cause);
    }
}
