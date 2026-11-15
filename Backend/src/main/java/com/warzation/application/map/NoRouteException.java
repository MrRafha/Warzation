package com.warzation.application.map;

public class NoRouteException extends RuntimeException {

    public NoRouteException(String from, String to) {
        super("Não existe rota terrestre entre " + from + " e " + to);
    }
}
