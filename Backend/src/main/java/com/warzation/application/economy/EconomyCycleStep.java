package com.warzation.application.economy;

import com.warzation.domain.match.Match;
import java.time.OffsetDateTime;

/**
 * Uma etapa aplicada a cada ciclo econômico de uma partida (produção, consumo,
 * conclusão de obras...). Roda dentro da transação do ciclo, com a partida
 * bloqueada: uma etapa nunca vê o mesmo ciclo duas vezes.
 */
public interface EconomyCycleStep {

    /**
     * @param cycle número do ciclo aplicado, começando em 1
     * @param at momento em que o ciclo venceu (não o momento do processamento,
     *           que pode ser bem depois durante a recuperação)
     */
    void apply(Match match, long cycle, OffsetDateTime at);
}
