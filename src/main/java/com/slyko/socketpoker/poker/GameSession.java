package com.slyko.socketpoker.poker;

import java.util.ArrayList;
import java.util.List;

public class GameSession {

    private String sessionId;
    private List<String> players = new ArrayList<>();  // Przykład listy graczy

    public GameSession(String sessionId) {
        this.sessionId = sessionId;
    }

    // Getter i setter dla sessionId oraz players
    public String getSessionId() {
        return sessionId;
    }

    public void addPlayer(String player) {
        players.add(player);
    }

    public List<String> getPlayers() {
        return players;
    }
}