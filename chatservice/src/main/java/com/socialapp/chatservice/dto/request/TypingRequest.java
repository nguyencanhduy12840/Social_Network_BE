package com.socialapp.chatservice.dto.request;

import lombok.Data;

@Data
public class TypingRequest {
    private String chatId;
    private boolean isTyping;
}
