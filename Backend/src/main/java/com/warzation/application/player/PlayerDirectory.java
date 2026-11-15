package com.warzation.application.player;

import com.warzation.config.PlayersProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolve tokens em jogadores. Os tokens ficam em memória só como hash SHA-256
 * e nunca são registrados em log.
 */
@Component
public class PlayerDirectory {

    public static final int MIN_TOKEN_LENGTH = 16;

    private static final Logger log = LoggerFactory.getLogger(PlayerDirectory.class);
    private static final String ID_PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$";

    private final Map<String, Player> playersByTokenHash;
    private final Map<String, Player> playersById;

    public PlayerDirectory(PlayersProperties properties) {
        List<String> problems = new ArrayList<>();
        Map<String, Player> byHash = new HashMap<>();
        Set<String> ids = new HashSet<>();

        for (PlayersProperties.TestPlayer configured : properties.testPlayers()) {
            String id = configured.id();
            if (id == null || !id.matches(ID_PATTERN)) {
                problems.add("id de jogador inválido: " + id);
                continue;
            }
            if (!ids.add(id)) {
                problems.add("id de jogador repetido: " + id);
            }
            String token = configured.token() == null ? "" : configured.token().trim();
            if (token.isEmpty()) {
                log.warn("Jogador de teste {} sem token configurado; ficará desativado", id);
                continue;
            }
            if (token.length() < MIN_TOKEN_LENGTH) {
                problems.add("token do jogador " + id + " precisa ter ao menos " + MIN_TOKEN_LENGTH + " caracteres");
                continue;
            }
            String name = configured.name() == null || configured.name().isBlank() ? id : configured.name().trim();
            if (byHash.put(hash(token), new Player(id, name)) != null) {
                problems.add("dois jogadores usam o mesmo token");
            }
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("Configuração de jogadores de teste inválida: " + String.join("; ", problems));
        }
        if (byHash.isEmpty()) {
            log.warn("Nenhum jogador de teste ativo: ordens serão recusadas até PLAYER_1_TOKEN/PLAYER_2_TOKEN serem definidos");
        } else {
            log.info("{} jogador(es) de teste ativo(s)", byHash.size());
        }
        this.playersByTokenHash = Map.copyOf(byHash);
        Map<String, Player> byId = new HashMap<>();
        byHash.values().forEach(player -> byId.put(player.id(), player));
        this.playersById = Map.copyOf(byId);
    }

    /** Jogador ativo pelo id, para exibir nomes; nunca serve para autenticar. */
    public Optional<Player> findById(String id) {
        return Optional.ofNullable(id == null ? null : playersById.get(id));
    }

    public Optional<Player> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(playersByTokenHash.get(hash(token.trim())));
    }

    public Player require(String token) {
        return authenticate(token).orElseThrow(() -> new UnauthenticatedException(
                token == null || token.isBlank() ? "Identifique-se com o token de um jogador" : "Token de jogador inválido"));
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
