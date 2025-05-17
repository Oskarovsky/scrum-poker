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

    public void broadcastSystemMessage(String roomId, MessageType type, String content) {
        ChatMessage msg = new ChatMessage(type, "SYSTEM", roomId, content);
        broadcastToRoom(roomId, msg);
    }

    public void broadcastUserStatus(String roomId, Map<String, Boolean> status) {
        try {
            String messageContent = objectMapper.writeValueAsString(status);
            ChatMessage userStatusMsg = new ChatMessage(MessageType.USERS, "SYSTEM", roomId, messageContent);
            broadcastToRoom(roomId, userStatusMsg);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize user status", e);
        }
    }

    public void broadcastToRoom(String roomId, ChatMessage message) {
        messagingTemplate.convertAndSend("/topic/" + roomId, message);
    }
}
