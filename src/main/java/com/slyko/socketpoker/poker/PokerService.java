package com.slyko.socketpoker.poker;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PokerService {

    public static final Map<String, String> POKER_MAP = new ConcurrentHashMap<>();

    void addUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            System.err.println("Cannot add user: userId is null or empty");
            return;
        }
        if (POKER_MAP.containsKey(userId)) {
            System.out.println("User already exists");
        } else {
            POKER_MAP.put(userId, "NIC");
        }
    }

    void updateVote(String userId, String vote) {
        if (userId == null || vote == null) {
            throw new IllegalArgumentException("User or vote cannot be null");
        }
        if (!POKER_MAP.containsKey(userId)) {
            System.out.println("User does not exist");
        } else {
            POKER_MAP.put(userId, vote);
        }
    }
}
