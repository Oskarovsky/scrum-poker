package com.slyko.socketpoker.controller;

import java.util.Map;

import com.slyko.socketpoker.poker.MessageType;
import com.slyko.socketpoker.service.PokerBroadcastService;
import com.slyko.socketpoker.service.PokerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class PokerRestController {

    private final PokerService pokerService;
    private final PokerBroadcastService broadcastService;

    @GetMapping("/votes/{roomId}")
    public Map<String, String> getVotes(@PathVariable String roomId) {
        return pokerService.getOnlyValidVotes(roomId);
    }

    @DeleteMapping("/votes/{roomId}")
    public void clearVotes(@PathVariable String roomId) {
        pokerService.clearVotes(roomId);
        broadcastService.broadcastSystemMessage(roomId, MessageType.CLEAR, "Głosy wyczyszczone.");
        broadcastService.broadcastUserStatus(roomId, pokerService.getVotingStatus(roomId));
    }

    @GetMapping("/users/{roomId}")
    public Map<String, Boolean> getUserStatus(@PathVariable String roomId) {
        return pokerService.getVotingStatus(roomId);
    }
}
