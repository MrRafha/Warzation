package com.warzation.application.match;

import com.warzation.application.CommandConflictException;
import com.warzation.application.InvalidCommandException;
import com.warzation.application.ResourceNotFoundException;
import com.warzation.application.map.LoadedScenario;
import com.warzation.application.map.ScenarioCatalog;
import com.warzation.application.map.ScenarioFile;
import com.warzation.application.player.Player;
import com.warzation.application.player.PlayerDirectory;
import com.warzation.domain.match.Match;
import com.warzation.domain.match.MatchCommand;
import com.warzation.domain.match.MatchCommandRepository;
import com.warzation.domain.match.MatchParticipant;
import com.warzation.domain.match.MatchParticipantRepository;
import com.warzation.domain.match.MatchRepository;
import com.warzation.domain.match.MatchStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Criação de partidas e entrada de jogadores. Cada ação é um comando com
 * commandId: o reenvio devolve o estado atual sem repetir o efeito.
 */
@Service
public class MatchService {

    public static final int MAX_NAME_LENGTH = 60;

    private final MatchRepository matches;
    private final MatchParticipantRepository participants;
    private final MatchCommandRepository commands;
    private final ScenarioCatalog scenarios;
    private final PlayerDirectory players;

    public MatchService(
            MatchRepository matches,
            MatchParticipantRepository participants,
            MatchCommandRepository commands,
            ScenarioCatalog scenarios,
            PlayerDirectory players) {
        this.matches = matches;
        this.participants = participants;
        this.commands = commands;
        this.scenarios = scenarios;
        this.players = players;
    }

    @Transactional
    public MatchCommandResult create(Player player, UUID commandId, String scenarioId, String name) {
        Optional<MatchCommand> previous = commands.findById(commandId);
        if (previous.isPresent()) {
            return duplicate(previous.get(), commandId, player, MatchCommand.CREATE_MATCH, null, null);
        }

        LoadedScenario scenario = scenarios.get(scenarioId);
        String finalName = name == null || name.isBlank() ? "Partida de " + player.name() : name.trim();
        if (finalName.length() > MAX_NAME_LENGTH) {
            throw new InvalidCommandException("Comando inválido",
                    Map.of("name", "no máximo " + MAX_NAME_LENGTH + " caracteres"));
        }

        Match match = matches.save(new Match(scenario.id(), finalName, scenario.file().nations().size(), player.id()));
        commands.save(new MatchCommand(commandId, player.id(), MatchCommand.CREATE_MATCH, match.getId(), null));
        return new MatchCommandResult(commandId, MatchCommandResult.Status.APPLIED, view(match));
    }

    @Transactional
    public MatchCommandResult join(Player player, UUID commandId, String matchId, String nationId) {
        Optional<MatchCommand> previous = commands.findById(commandId);
        if (previous.isPresent()) {
            return duplicate(previous.get(), commandId, player, MatchCommand.JOIN_MATCH, matchId, nationId);
        }

        Match match = matches.findByIdForUpdate(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida não encontrada: " + matchId));
        LoadedScenario scenario = scenarios.get(match.getScenarioId());
        boolean nationExists = scenario.file().nations().stream().anyMatch(n -> n.id().equals(nationId));
        if (!nationExists) {
            throw new InvalidCommandException("Comando inválido",
                    Map.of("nationId", "nação inexistente no cenário " + scenario.id()));
        }
        if (match.getStatus() != MatchStatus.OPEN) {
            throw new MatchRuleException(MatchRuleException.Reason.MATCH_NOT_OPEN,
                    "A partida não está aceitando jogadores (" + match.getStatus() + ")");
        }

        List<MatchParticipant> current = participants.findByIdMatchIdOrderByJoinedAtAsc(match.getId());
        if (current.stream().anyMatch(p -> p.getPlayerId().equals(player.id()))) {
            throw new MatchRuleException(MatchRuleException.Reason.ALREADY_JOINED,
                    "Você já está nesta partida");
        }
        if (current.size() >= match.getMaxPlayers()) {
            throw new MatchRuleException(MatchRuleException.Reason.MATCH_FULL, "A partida está cheia");
        }
        if (current.stream().anyMatch(p -> p.getNationId().equals(nationId))) {
            throw new MatchRuleException(MatchRuleException.Reason.NATION_TAKEN,
                    "A nação " + nationId + " já foi escolhida por outro jogador");
        }

        participants.save(new MatchParticipant(match, player.id(), nationId));
        match.touch();
        commands.save(new MatchCommand(commandId, player.id(), MatchCommand.JOIN_MATCH, match.getId(), nationId));
        return new MatchCommandResult(commandId, MatchCommandResult.Status.APPLIED, view(match));
    }

    @Transactional(readOnly = true)
    public List<MatchView> list(MatchStatus status) {
        List<Match> found = status == null
                ? matches.findAllByOrderByCreatedAtDesc()
                : matches.findByStatusOrderByCreatedAtDesc(status);
        return found.stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public MatchView get(String matchId) {
        return matches.findById(matchId)
                .map(this::view)
                .orElseThrow(() -> new ResourceNotFoundException("Partida não encontrada: " + matchId));
    }

    private MatchCommandResult duplicate(
            MatchCommand previous, UUID commandId, Player player, String type, String matchId, String nationId) {
        if (!previous.sameIntent(player.id(), type, matchId, nationId)) {
            throw new CommandConflictException("O commandId " + commandId + " já foi usado por outro comando");
        }
        Match match = matches.findById(previous.getMatchId()).orElseThrow();
        return new MatchCommandResult(commandId, MatchCommandResult.Status.DUPLICATE, view(match));
    }

    private MatchView view(Match match) {
        List<MatchParticipant> joined = participants.findByIdMatchIdOrderByJoinedAtAsc(match.getId());
        Map<String, String> playerByNation = joined.stream()
                .collect(Collectors.toMap(MatchParticipant::getNationId, MatchParticipant::getPlayerId));
        List<ScenarioFile.Nation> nations = scenarios.get(match.getScenarioId()).file().nations();
        Function<String, String> playerName = id -> players.findById(id).map(Player::name).orElse(id);

        return new MatchView(
                match.getId(),
                match.getName(),
                match.getScenarioId(),
                match.getStatus().name(),
                match.getMaxPlayers(),
                match.getCreatedBy(),
                match.getCreatedAt(),
                joined.stream()
                        .map(p -> new MatchView.Participant(p.getPlayerId(), playerName.apply(p.getPlayerId()),
                                p.getNationId(), p.getJoinedAt()))
                        .toList(),
                nations.stream()
                        .map(n -> new MatchView.Nation(n.id(), n.name(), n.capital(), playerByNation.get(n.id())))
                        .toList());
    }
}
