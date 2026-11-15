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
import com.warzation.domain.match.MatchEvent;
import com.warzation.domain.match.MatchEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warzation.domain.match.MatchParticipant;
import com.warzation.domain.match.MatchParticipantRepository;
import com.warzation.domain.match.MatchRepository;
import com.warzation.domain.match.MatchProvince;
import com.warzation.domain.match.MatchProvinceRepository;
import com.warzation.domain.match.MatchProvinceStock;
import com.warzation.domain.match.MatchProvinceStockRepository;
import com.warzation.domain.match.MatchStatus;
import com.warzation.domain.match.MatchUnit;
import com.warzation.domain.match.MatchUnitRepository;
import com.warzation.config.MatchRulesProperties;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final MatchProvinceRepository matchProvinces;
    private final MatchUnitRepository matchUnits;
    private final MatchProvinceStockRepository matchStocks;
    private final MatchRulesProperties rules;
    private final ApplicationEventPublisher events;
    private final MatchEventLog eventLog;
    private final MatchEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public MatchService(
            MatchRepository matches,
            MatchParticipantRepository participants,
            MatchCommandRepository commands,
            ScenarioCatalog scenarios,
            PlayerDirectory players,
            MatchProvinceRepository matchProvinces,
            MatchUnitRepository matchUnits,
            MatchProvinceStockRepository matchStocks,
            MatchRulesProperties rules,
            ApplicationEventPublisher events,
            MatchEventLog eventLog,
            MatchEventRepository eventRepository,
            ObjectMapper objectMapper) {
        this.matches = matches;
        this.participants = participants;
        this.commands = commands;
        this.scenarios = scenarios;
        this.players = players;
        this.matchProvinces = matchProvinces;
        this.matchUnits = matchUnits;
        this.matchStocks = matchStocks;
        this.rules = rules;
        this.events = events;
        this.eventLog = eventLog;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
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
        eventLog.record(match.getId(), MatchEvent.MATCH_CREATED, player.id(), commandId,
                Map.of("name", match.getName(), "scenarioId", match.getScenarioId()));
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
        eventLog.record(match.getId(), MatchEvent.PLAYER_JOINED, player.id(), commandId, Map.of("nationId", nationId));
        events.publishEvent(new MatchChanged(match.getId()));
        return new MatchCommandResult(commandId, MatchCommandResult.Status.APPLIED, view(match));
    }

    /** Inicia a partida e aplica a posição inicial. Só quem criou pode iniciar. */
    @Transactional
    public MatchCommandResult start(Player player, UUID commandId, String matchId) {
        Optional<MatchCommand> previous = commands.findById(commandId);
        if (previous.isPresent()) {
            return duplicate(previous.get(), commandId, player, MatchCommand.START_MATCH, matchId, null);
        }

        Match match = matches.findByIdForUpdate(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida não encontrada: " + matchId));
        if (!match.getCreatedBy().equals(player.id())) {
            throw new MatchRuleException(MatchRuleException.Reason.NOT_MATCH_CREATOR,
                    "Só quem criou a partida pode iniciá-la");
        }
        if (match.getStatus() != MatchStatus.OPEN) {
            throw new MatchRuleException(MatchRuleException.Reason.MATCH_NOT_OPEN,
                    "A partida já começou ou foi encerrada (" + match.getStatus() + ")");
        }
        List<MatchParticipant> joined = participants.findByIdMatchIdOrderByJoinedAtAsc(match.getId());
        if (joined.size() < rules.minPlayersToStart()) {
            throw new MatchRuleException(MatchRuleException.Reason.NOT_ENOUGH_PLAYERS,
                    "São necessários ao menos " + rules.minPlayersToStart() + " jogadores para iniciar");
        }

        Set<String> claimedNations = joined.stream().map(MatchParticipant::getNationId).collect(Collectors.toSet());
        InitialPosition.Result position = InitialPosition.build(
                match.getId(), scenarios.get(match.getScenarioId()), claimedNations, rules);
        matchProvinces.saveAll(position.provinces());
        matchProvinces.flush();
        matchUnits.saveAll(position.units());
        matchStocks.saveAll(position.stocks());
        match.start();
        commands.save(new MatchCommand(commandId, player.id(), MatchCommand.START_MATCH, match.getId(), null));
        eventLog.record(match.getId(), MatchEvent.MATCH_STARTED, player.id(), commandId, Map.of(
                "nations", claimedNations.stream().sorted().toList(),
                "neutralProvinces", position.provinces().stream().filter(p -> p.getOwnerNationId() == null).count()));
        events.publishEvent(new MatchChanged(match.getId()));
        return new MatchCommandResult(commandId, MatchCommandResult.Status.APPLIED, view(match));
    }

    /** Estado territorial; só participantes podem ver. */
    @Transactional(readOnly = true)
    public MatchStateView state(Player player, String matchId) {
        Match match = matches.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida não encontrada: " + matchId));
        requireParticipant(player, match);
        return stateOf(match);
    }

    /** Partida + estado para um participante (canal em tempo real). */
    @Transactional(readOnly = true)
    public MatchSnapshot snapshotFor(Player player, String matchId) {
        Match match = matches.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida não encontrada: " + matchId));
        requireParticipant(player, match);
        return new MatchSnapshot(view(match), stateOf(match));
    }

    /**
     * Partida + estado para difusão. Roda em transação nova porque é chamada
     * depois do commit da transação que alterou a partida.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public MatchSnapshot snapshot(String matchId) {
        Match match = matches.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida não encontrada: " + matchId));
        return new MatchSnapshot(view(match), stateOf(match));
    }

    /** Histórico em ordem; visível para participantes e para quem criou a partida. */
    @Transactional(readOnly = true)
    public List<MatchEventView> events(Player player, String matchId) {
        Match match = matches.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida não encontrada: " + matchId));
        if (!match.getCreatedBy().equals(player.id())) {
            requireParticipant(player, match);
        }
        return eventRepository.findByMatchIdOrderBySeqAsc(matchId).stream()
                .map(event -> new MatchEventView(
                        event.getSeq(),
                        event.getEventType(),
                        event.getPlayerId(),
                        event.getPlayerId() == null ? null
                                : players.findById(event.getPlayerId()).map(Player::name).orElse(event.getPlayerId()),
                        event.getCommandId(),
                        event.getOccurredAt(),
                        readJson(event.getData())))
                .toList();
    }

    private com.fasterxml.jackson.databind.JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Evento com dados inválidos", exception);
        }
    }

    /** Lança NOT_PARTICIPANT se o jogador não estiver na partida. */
    @Transactional(readOnly = true)
    public void requireParticipant(Player player, String matchId) {
        Match match = matches.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Partida não encontrada: " + matchId));
        requireParticipant(player, match);
    }

    private void requireParticipant(Player player, Match match) {
        boolean participant = participants.findByIdMatchIdOrderByJoinedAtAsc(match.getId()).stream()
                .anyMatch(p -> p.getPlayerId().equals(player.id()));
        if (!participant) {
            throw new MatchRuleException(MatchRuleException.Reason.NOT_PARTICIPANT,
                    "Você não participa desta partida");
        }
    }

    private MatchStateView stateOf(Match match) {
        Map<String, String> playerByNation = participants.findByIdMatchIdOrderByJoinedAtAsc(match.getId()).stream()
                .collect(Collectors.toMap(MatchParticipant::getNationId, MatchParticipant::getPlayerId));
        Map<String, List<MatchUnit>> unitsByProvince = matchUnits.findByMatchId(match.getId()).stream()
                .collect(Collectors.groupingBy(MatchUnit::getProvinceId));
        Map<String, List<MatchProvinceStock>> stockByProvince = matchStocks.findByIdMatchId(match.getId()).stream()
                .collect(Collectors.groupingBy(MatchProvinceStock::getProvinceId));
        Map<String, MatchProvince> byId = matchProvinces.findByIdMatchId(match.getId()).stream()
                .collect(Collectors.toMap(MatchProvince::getProvinceId, Function.identity()));

        // Mesma ordem do cenário, para respostas estáveis.
        List<MatchStateView.Province> provinces = scenarios.get(match.getScenarioId()).file().provinces().stream()
                .map(ScenarioFile.Province::id)
                .filter(byId::containsKey)
                .map(id -> {
                    MatchProvince province = byId.get(id);
                    Map<String, Long> stock = new LinkedHashMap<>();
                    stockByProvince.getOrDefault(id, List.of()).stream()
                            .sorted(java.util.Comparator.comparing(MatchProvinceStock::getResource))
                            .forEach(s -> stock.put(s.getResource(), s.getAmount()));
                    return new MatchStateView.Province(
                            id,
                            province.getOwnerNationId(),
                            province.getOwnerNationId() == null ? null : playerByNation.get(province.getOwnerNationId()),
                            unitsByProvince.getOrDefault(id, List.of()).stream()
                                    .map(u -> new MatchStateView.Units(u.getUnitType(), u.getQuantity(), u.getOwnerNationId()))
                                    .toList(),
                            stock);
                })
                .toList();
        return new MatchStateView(match.getId(), match.getStatus().name(), match.getStartedAt(), provinces);
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
                match.getStartedAt(),
                joined.stream()
                        .map(p -> new MatchView.Participant(p.getPlayerId(), playerName.apply(p.getPlayerId()),
                                p.getNationId(), p.getJoinedAt()))
                        .toList(),
                nations.stream()
                        .map(n -> new MatchView.Nation(n.id(), n.name(), n.capital(), playerByNation.get(n.id())))
                        .toList());
    }
}
