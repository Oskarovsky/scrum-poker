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

import static com.slyko.socketpoker.poker.MessageType.LEAVE;
import static com.slyko.socketpoker.poker.MessageType.USERS;
import static com.slyko.socketpoker.poker.PokerService.POKER_MAP;

@Controller
@RequiredArgsConstructor
public class PokerController {

    private final PokerService pokerService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public void sendMessage(
            @Payload ChatMessage chatMessage
    ) {
        String roomId = chatMessage.getRoomId();
        if (roomId == null) return;

        if (chatMessage.getType() == LEAVE) {
            pokerService.removeUser(roomId, chatMessage.getSender());
        }

        pokerService.updateVote(roomId, chatMessage.getSender(), chatMessage.getContent());
        messagingTemplate.convertAndSend("/topic/" + roomId, chatMessage);
        broadcastUserStatus(roomId);
    }


    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String roomId = chatMessage.getRoomId();
        if (roomId == null) return;

        pokerService.addUser(roomId, chatMessage.getSender());
        Objects.requireNonNull(headerAccessor.getSessionAttributes()).put("username", chatMessage.getSender());
        messagingTemplate.convertAndSend("/topic/" + roomId, chatMessage);
        broadcastUserStatus(roomId);
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