package com.warzation.application;

/** O commandId já foi usado por um comando com outra intenção (sala, tipo ou conteúdo). */
public class CommandConflictException extends RuntimeException {

    public CommandConflictException(String message) {
        super(message);
    }
}
