package com.socialapp.chatservice.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TypingRequest {
    private String chatId;
    
    @JsonProperty("isTyping")
    private boolean typing;
}
