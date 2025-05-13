package com.slyko.socketpoker.poker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PokerService {

    // roomId -> (userId -> vote)
    public static final Map<String, Map<String, String>> ROOM_MAP = new ConcurrentHashMap<>();

    public static final Map<String, String> POKER_MAP = new ConcurrentHashMap<>();

    public void addUser(String roomId, String userId) {
        if (roomId == null || userId == null || userId.trim().isEmpty()) return;
        log.info("add user {} to room {}", userId, roomId);
        ROOM_MAP.putIfAbsent(roomId, new ConcurrentHashMap<>());
        ROOM_MAP.get(roomId).putIfAbsent(userId, "NIC");
    }


    public void updateVote(String roomId, String userId, String vote) {
        if (roomId == null || userId == null || vote == null) return;
        log.info("update vote {} to room {}", vote, roomId);
        ROOM_MAP.putIfAbsent(roomId, new ConcurrentHashMap<>());
        ROOM_MAP.get(roomId).put(userId, vote);
    }

    public void clearVotes(String roomId) {
        log.info("clear votes from room {}", roomId);
        if (ROOM_MAP.containsKey(roomId)) {
            ROOM_MAP.get(roomId).replaceAll((user, val) -> "NIC");
        }
    }

    public Map<String, String> getVotes(String roomId) {
        log.info("get votes from room {}", roomId);
        return ROOM_MAP.getOrDefault(roomId, Map.of());
    }

    public Map<String, Boolean> getVotingStatus(String roomId) {
        log.info("get voting status from room {}", roomId);
        return ROOM_MAP.getOrDefault(roomId, Map.of()).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> !"NIC".equals(e.getValue())));
    }

    // ✅ NOWA METODA: usuń użytkownika z pokoju
    public void removeUser(String roomId, String userId) {
        if (roomId == null || userId == null) return;
        log.info("remove user {} from room {}", userId, roomId);
        Map<String, String> users = ROOM_MAP.get(roomId);
        if (users != null) {
            users.remove(userId);
            // Jeśli pokój jest pusty, możesz też go wyczyścić z mapy:
            if (users.isEmpty()) {
                ROOM_MAP.remove(roomId);
                log.info("room {} is now empty and was removed", roomId);
            }
        }
    }
}
