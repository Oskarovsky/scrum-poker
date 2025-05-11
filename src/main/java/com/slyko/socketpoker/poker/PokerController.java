package com.slyko.socketpoker.poker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.slyko.socketpoker.poker.MessageType.USERS;
import static com.slyko.socketpoker.poker.PokerService.POKER_MAP;

@Controller
@RequiredArgsConstructor
public class PokerController {

    private final PokerService pokerService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(
            @Payload ChatMessage chatMessage
    ) {
        if (chatMessage.getSender() != null && chatMessage.getContent() != null) {
            pokerService.updateVote(chatMessage.getSender(), chatMessage.getContent());
            broadcastUserStatus();
        }
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(
            @Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        // Add username in web socket session
        pokerService.addUser(chatMessage.getSender());
        Objects.requireNonNull(headerAccessor.getSessionAttributes()).put("username", chatMessage.getSender());
        broadcastUserStatus();
        return chatMessage;
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