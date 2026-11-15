package com.warzation.application.player;

/** Jogador identificado pelo servidor. Nunca vem do corpo de uma mensagem do cliente. */
public record Player(String id, String name) {
}
