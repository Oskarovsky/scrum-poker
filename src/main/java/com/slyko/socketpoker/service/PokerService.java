package com.slyko.socketpoker.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PokerService {

    // roomId -> (userId -> vote)
    private final Map<String, Map<String, String>> roomVotes = new ConcurrentHashMap<>();

    public void addUser(String roomId, String userId) {
        if (roomId == null || userId == null || userId.trim().isEmpty()) return;
        log.info("add user {} to room {}", userId, roomId);
        roomVotes.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).putIfAbsent(userId, "NIC");
    }

    public void updateVote(String roomId, String userId, String vote) {
        if (roomId == null || userId == null || vote == null) return;
        log.info("update vote {} from user {} in room {}", vote, userId, roomId);
        roomVotes.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, vote);
    }

    public void clearVotes(String roomId) {
        log.info("clear votes from room {}", roomId);
        Map<String, String> users = roomVotes.get(roomId);
        if (users != null) {
            users.replaceAll((k, v) -> "NIC");
        }
    }

    public void removeUser(String roomId, String userId) {
        if (roomId == null || userId == null) return;
        Map<String, String> users = roomVotes.get(roomId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                roomVotes.remove(roomId);
                log.info("room {} is now empty and was removed", roomId);
            }
        }
    }

    public Map<String, String> getAllVotes(String roomId) {
        return roomVotes.getOrDefault(roomId, Map.of());
    }

    public Map<String, String> getOnlyValidVotes(String roomId) {
        return getAllVotes(roomId).entrySet().stream()
                .filter(e -> !"NIC".equals(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, Boolean> getVotingStatus(String roomId) {
        return getAllVotes(roomId).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> !"NIC".equals(e.getValue())));
    }
}
