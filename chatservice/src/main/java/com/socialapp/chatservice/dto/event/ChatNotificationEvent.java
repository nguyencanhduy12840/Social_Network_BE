package com.socialapp.chatservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatNotificationEvent {
    private String messageId;
    private String chatId;
    private String senderId;
    private String receiverId;
    private String content;
    private String eventType;
}
