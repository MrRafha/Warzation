package com.warzation.application.player;

/** Requisição ou sessão sem identidade de jogador válida. */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException(String message) {
        super(message);
    }
}
