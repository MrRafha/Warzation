package com.warzation.application.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.warzation.config.PlayersProperties;
import com.warzation.config.PlayersProperties.TestPlayer;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerDirectoryTests {

    private static PlayerDirectory directory(TestPlayer... players) {
        return new PlayerDirectory(new PlayersProperties(List.of(players)));
    }

    @Test
    void resolvesConfiguredTokens() {
        PlayerDirectory players = directory(
                new TestPlayer("jogador-1", "Jogador 1", "token-do-jogador-um"),
                new TestPlayer("jogador-2", "Jogador 2", "token-do-jogador-dois"));

        assertThat(players.authenticate("token-do-jogador-um")).contains(new Player("jogador-1", "Jogador 1"));
        assertThat(players.authenticate(" token-do-jogador-dois ")).contains(new Player("jogador-2", "Jogador 2"));
        assertThat(players.authenticate("token-que-nao-existe")).isEmpty();
        assertThat(players.authenticate(null)).isEmpty();
        assertThat(players.authenticate("")).isEmpty();
    }

    @Test
    void requireExplainsWhatIsMissing() {
        PlayerDirectory players = directory(new TestPlayer("jogador-1", "Jogador 1", "token-do-jogador-um"));

        assertThatThrownBy(() -> players.require(null))
                .isInstanceOf(UnauthenticatedException.class).hasMessageContaining("Identifique-se");
        assertThatThrownBy(() -> players.require("errado-errado-errado"))
                .isInstanceOf(UnauthenticatedException.class).hasMessage("Token de jogador inválido");
    }

    @Test
    void playerWithoutTokenIsDisabled() {
        PlayerDirectory players = directory(
                new TestPlayer("jogador-1", "Jogador 1", ""),
                new TestPlayer("jogador-2", "Jogador 2", null));

        assertThat(players.authenticate("")).isEmpty();
    }

    @Test
    void rejectsWeakDuplicatedOrMalformedConfiguration() {
        assertThatThrownBy(() -> directory(new TestPlayer("jogador-1", "J", "curto")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("16 caracteres");
        assertThatThrownBy(() -> directory(
                new TestPlayer("jogador-1", "A", "token-repetido-123"),
                new TestPlayer("jogador-2", "B", "token-repetido-123")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("mesmo token");
        assertThatThrownBy(() -> directory(
                new TestPlayer("jogador-1", "A", "token-do-jogador-a"),
                new TestPlayer("jogador-1", "B", "token-do-jogador-b")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("repetido");
        assertThatThrownBy(() -> directory(new TestPlayer("Jogador Um", "A", "token-do-jogador-a")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("id de jogador inválido");
    }

    @Test
    void errorsNeverContainTheToken() {
        assertThatThrownBy(() -> directory(new TestPlayer("jogador-1", "J", "segredo")))
                .hasMessageNotContaining("segredo");
    }
}
