package com.slyko.socketpoker.poker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

import static com.slyko.socketpoker.poker.MessageType.*;
import static com.slyko.socketpoker.poker.PokerService.POKER_MAP;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PokerRestController {

    private final PokerService pokerService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/votes/{roomId}")
    public Map<String, String> getVotes(@PathVariable String roomId) {
        return pokerService.getOnlyValidVotes(roomId); // <-- używamy nowej metody
    }

    @DeleteMapping("/votes/{roomId}")
    public void clearVotes(@PathVariable String roomId) {
        pokerService.clearVotes(roomId);

        ChatMessage clearMessage = new ChatMessage();
        clearMessage.setType(CLEAR);
        clearMessage.setSender("SYSTEM");
        clearMessage.setContent("Głosy wyczyszczone.");
        clearMessage.setRoomId(roomId);

        messagingTemplate.convertAndSend("/topic/" + roomId, clearMessage);
        broadcastUserStatus(roomId);
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

    @GetMapping("/users/{roomId}")
    public Map<String, Boolean> getUsersStatus(@PathVariable String roomId) {
        return pokerService.getVotingStatus(roomId);
    }

    private void broadcastUserStatus(String roomId) {
        Map<String, Boolean> status = pokerService.getVotingStatus(roomId);
        ChatMessage userStatusMsg = new ChatMessage();
        userStatusMsg.setType(USERS);
        userStatusMsg.setSender("SYSTEM");
        userStatusMsg.setRoomId(roomId);
        try {
            userStatusMsg.setContent(new ObjectMapper().writeValueAsString(status));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        messagingTemplate.convertAndSend("/topic/" + roomId, userStatusMsg);
    }
}
