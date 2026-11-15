package com.warzation.application.rules;

import java.util.List;

/** Regras de jogo inválidas: impedem o startup. */
public class GameRulesValidationException extends RuntimeException {

    private final String source;
    private final transient List<String> problems;

    public GameRulesValidationException(String source, List<String> problems) {
        super("Regras de jogo inválidas em " + source + ": " + String.join("; ", problems));
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
