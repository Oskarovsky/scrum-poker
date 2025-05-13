package com.slyko.socketpoker.poker;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping
public class GameController {

    @GetMapping("/api/generateGame")
    public ResponseEntity<Map<String, String>> generateGame() {
        String sessionId = UUID.randomUUID().toString();
        Map<String, String> response = new HashMap<>();
        response.put("sessionId", sessionId);
        return ResponseEntity.ok(response);
    }

    @RequestMapping("/game/{sessionId}")
    public String joinGame(@PathVariable("sessionId") String sessionId, Model model) {
        model.addAttribute("sessionId", sessionId);  // Przekaż sessionId do widoku HTML
        return "game";  // Przekierowanie do widoku /game
    }
}