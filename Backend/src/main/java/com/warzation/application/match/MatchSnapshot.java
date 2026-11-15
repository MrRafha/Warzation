package com.warzation.application.match;

/** O que um participante recebe ao entrar no canal da partida e a cada mudança. */
public record MatchSnapshot(MatchView match, MatchStateView state) {
}
