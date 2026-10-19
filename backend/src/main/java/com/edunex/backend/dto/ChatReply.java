package com.edunex.backend.dto;

public record ChatReply(Long sessionId, String title, ChatMessageResponse userMessage, ChatMessageResponse assistantMessage) {
}