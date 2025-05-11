package com.slyko.socketpoker.poker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

import static com.slyko.socketpoker.poker.MessageType.*;
import static com.slyko.socketpoker.poker.PokerService.POKER_MAP;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PokerRestController {

    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/votes")
    public Map<String, String> getAllVotes() {
        return POKER_MAP;
    }

    @DeleteMapping("/votes")
    public void clearVotes() {
        POKER_MAP.forEach((key, value) -> POKER_MAP.put(key, "NIC"));

        // Broadcast CLEAR event to all users via WebSocket
        ChatMessage clearMessage = new ChatMessage();
        clearMessage.setType(CLEAR);
        clearMessage.setSender("SYSTEM");
        clearMessage.setContent("Wszystkie głosy zostały wyczyszczone.");

        messagingTemplate.convertAndSend("/topic/public", clearMessage);
        broadcastUserStatus();
    }

    // Endpoint do wysyłania wszystkich głosów do wszystkich użytkowników
    @GetMapping("/broadcastVotes")
    public void broadcastVotes() {
        // Stwórz wiadomość zawierającą wszystkie głosy
        StringBuilder voteMessage = new StringBuilder("Głosy wszystkich użytkowników:\n");
        POKER_MAP.forEach((key, value) -> voteMessage.append(key).append(": ").append(value).append("\n"));

        // Broadcast vote information to all users
        ChatMessage voteBroadcastMessage = new ChatMessage();
        voteBroadcastMessage.setType(VOTES);
        voteBroadcastMessage.setSender("SYSTEM");
        voteBroadcastMessage.setContent(voteMessage.toString());

        messagingTemplate.convertAndSend("/topic/public", voteBroadcastMessage);
    }

    @GetMapping("/users")
    public Map<String, Boolean> getUsersVotingStatus() {
        // true = już zagłosował (ma wartość), false = brak głosu
        return POKER_MAP.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() != null && !e.getValue().isEmpty()
                ));
    }

    private void broadcastUserStatus() {
        Map<String, Boolean> status = POKER_MAP.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() != null && !e.getValue().isEmpty()
                ));

        ChatMessage userStatusMsg = new ChatMessage();
        userStatusMsg.setType(USERS);
        userStatusMsg.setSender("SYSTEM");
        try {
            userStatusMsg.setContent(new ObjectMapper().writeValueAsString(status)); // JSON
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        messagingTemplate.convertAndSend("/topic/public", userStatusMsg);
    }
}
