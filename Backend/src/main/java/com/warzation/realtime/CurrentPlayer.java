package com.warzation.realtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injeta o jogador identificado pelo cabeçalho {@code Authorization: Bearer <token>}.
 * Sem identidade válida, a requisição é recusada com 401.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentPlayer {
}
