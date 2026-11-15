package com.warzation.realtime;

import com.warzation.application.player.Player;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    /** Confirma quem é o jogador do token enviado. */
    @GetMapping("/me")
    public Player me(@CurrentPlayer Player player) {
        return player;
    }
}
