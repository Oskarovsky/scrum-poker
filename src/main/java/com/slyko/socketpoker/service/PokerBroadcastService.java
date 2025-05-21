package com.slyko.socketpoker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

import com.slyko.socketpoker.model.ChatMessage;
import com.slyko.socketpoker.model.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PokerBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void broadcastToRoom(String roomId, ChatMessage message) {
        messagingTemplate.convertAndSend("/topic/" + roomId, message);
    }

    public void broadcastSystemMessage(String roomId, MessageType type, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setType(type);
        msg.setSender("SYSTEM");
        msg.setRoomId(roomId);
        msg.setContent(content);
        broadcastToRoom(roomId, msg);
    }

    public void broadcastUserStatus(String roomId, Map<String, Boolean> status) {
        ChatMessage userStatusMsg = new ChatMessage();
        userStatusMsg.setType(MessageType.USERS);
        userStatusMsg.setSender("SYSTEM");
        userStatusMsg.setRoomId(roomId);
        try {
            userStatusMsg.setContent(objectMapper.writeValueAsString(status));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize user status", e);
        }
        broadcastToRoom(roomId, userStatusMsg);
    }
}
