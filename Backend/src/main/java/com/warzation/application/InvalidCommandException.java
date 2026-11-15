package com.warzation.application;

import java.util.Map;

/** Comando rejeitado na validação; nenhum estado é alterado. */
public class InvalidCommandException extends RuntimeException {

    private final transient Map<String, String> fields;

    public InvalidCommandException(String message, Map<String, String> fields) {
        super(message);
        this.fields = Map.copyOf(fields);
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
