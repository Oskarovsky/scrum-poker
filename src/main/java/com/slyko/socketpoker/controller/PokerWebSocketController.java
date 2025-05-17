package com.slyko.socketpoker.controller;

import com.slyko.socketpoker.model.ChatMessage;
import com.slyko.socketpoker.model.MessageType;
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
        String roomId = chatMessage.roomId();
        if (roomId == null) return;

        pokerService.addUser(roomId, chatMessage.sender());
        headerAccessor.getSessionAttributes().put("username", chatMessage.sender());
        broadcastService.broadcastToRoom(roomId, chatMessage);
        broadcastService.broadcastUserStatus(roomId, pokerService.getVotingStatus(roomId));
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        String roomId = chatMessage.roomId();
        if (roomId == null) return;

        if (chatMessage.type() == MessageType.LEAVE) {
            pokerService.removeUser(roomId, chatMessage.sender());
        }

        pokerService.updateVote(roomId, chatMessage.sender(), chatMessage.content());
        broadcastService.broadcastToRoom(roomId, chatMessage);
        broadcastService.broadcastUserStatus(roomId, pokerService.getVotingStatus(roomId));
    }
}
