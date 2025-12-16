package com.socialapp.chatservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TypingEvent {
    private String chatId;
    private String senderId;
    private String recipientId;
    private boolean isTyping;
}
