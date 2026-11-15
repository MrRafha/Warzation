package com.warzation.application.map;

import java.util.List;

/** Dados de cenário inválidos: impedem a carga e, portanto, o startup. */
public class ScenarioValidationException extends RuntimeException {

    private final String source;
    private final transient List<String> problems;

    public ScenarioValidationException(String source, List<String> problems) {
        super("Cenário inválido em " + source + ": " + String.join("; ", problems));
        this.source = source;
        this.problems = List.copyOf(problems);
    }

    public String getSource() {
        return source;
    }

    public List<String> getProblems() {
        return problems;
    }
}
