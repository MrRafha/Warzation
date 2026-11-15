package com.warzation.realtime;

import com.warzation.application.match.MatchCommandResult;
import com.warzation.application.match.MatchService;
import com.warzation.application.match.MatchView;
import com.warzation.application.player.Player;
import com.warzation.domain.match.MatchStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    public record CreateMatchRequest(
            @NotBlank @Pattern(regexp = UUID_PATTERN, message = "deve ser um UUID") String commandId,
            @NotBlank String scenarioId,
            @Size(max = MatchService.MAX_NAME_LENGTH) String name) {
    }

    public record JoinMatchRequest(
            @NotBlank @Pattern(regexp = UUID_PATTERN, message = "deve ser um UUID") String commandId,
            @NotBlank String nationId) {
    }

    @GetMapping
    public List<MatchView> list(@RequestParam(required = false) MatchStatus status) {
        return matchService.list(status);
    }

    @GetMapping("/{matchId}")
    public MatchView get(@PathVariable String matchId) {
        return matchService.get(matchId);
    }

    @PostMapping
    public ResponseEntity<MatchCommandResult> create(
            @CurrentPlayer Player player,
            @Valid @RequestBody CreateMatchRequest request) {
        return respond(matchService.create(
                player, UUID.fromString(request.commandId()), request.scenarioId(), request.name()));
    }

    @PostMapping("/{matchId}/participants")
    public ResponseEntity<MatchCommandResult> join(
            @CurrentPlayer Player player,
            @PathVariable String matchId,
            @Valid @RequestBody JoinMatchRequest request) {
        return respond(matchService.join(player, UUID.fromString(request.commandId()), matchId, request.nationId()));
    }

    private static ResponseEntity<MatchCommandResult> respond(MatchCommandResult result) {
        HttpStatus status = result.status() == MatchCommandResult.Status.APPLIED ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result);
    }
}
