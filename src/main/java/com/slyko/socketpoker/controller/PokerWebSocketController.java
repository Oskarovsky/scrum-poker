package com.slyko.socketpoker.controller;

import com.slyko.socketpoker.poker.ChatMessage;
import com.slyko.socketpoker.poker.MessageType;
import com.slyko.socketpoker.service.PokerBroadcastService;
import com.slyko.socketpoker.service.PokerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PokerWebSocketController {

    private final PokerService pokerService;
    private final PokerBroadcastService broadcastService;

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String roomId = chatMessage.getRoomId();
        if (roomId == null) return;

        pokerService.addUser(roomId, chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        broadcastService.broadcastToRoom(roomId, chatMessage);
        broadcastService.broadcastUserStatus(roomId, pokerService.getVotingStatus(roomId));
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        String roomId = chatMessage.getRoomId();
        if (roomId == null) return;

        if (chatMessage.getType() == MessageType.LEAVE) {
            pokerService.removeUser(roomId, chatMessage.getSender());
        }

        pokerService.updateVote(roomId, chatMessage.getSender(), chatMessage.getContent());
        broadcastService.broadcastToRoom(roomId, chatMessage);
        broadcastService.broadcastUserStatus(roomId, pokerService.getVotingStatus(roomId));
    }
}
