package com.warzation.application.match;

/** Publicado quando algo muda numa partida; os participantes recebem o novo estado após o commit. */
public record MatchChanged(String matchId) {
}
