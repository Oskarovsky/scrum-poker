package com.slyko.socketpoker.model;

import lombok.Builder;

@Builder
public record ChatMessage(
    MessageType type,
    String content,
    String sender,
    String roomId
) {
}