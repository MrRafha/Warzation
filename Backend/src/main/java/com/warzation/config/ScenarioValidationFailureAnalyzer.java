package com.warzation.config;

import com.warzation.application.map.ScenarioValidationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Lista, no startup, cada problema encontrado nos dados de um cenário. */
public class ScenarioValidationFailureAnalyzer extends AbstractFailureAnalyzer<ScenarioValidationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ScenarioValidationException cause) {
        StringBuilder description = new StringBuilder("Os dados do cenário em ")
                .append(cause.getSource())
                .append(" são inválidos:");
        cause.getProblems().forEach(problem -> description.append(System.lineSeparator()).append("  - ").append(problem));
        String action = "Corrija os arquivos em Backend/data/scenarios e valide com "
                + "'python tools/mapdata/validate_scenario.py <pasta do cenário>'. "
                + "O formato está em Backend/data/README.md.";
        return new FailureAnalysis(description.toString(), action, cause);
    }
}
