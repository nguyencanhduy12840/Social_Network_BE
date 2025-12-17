package com.socialapp.chatservice.dto.event;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessageEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String messageId;
    private String chatId;
    private String senderId;
    private String senderName;
    private String senderAvatar;
    private String recipientId; 
    private String content;
    private List<String> attachments;
    private Instant createdAt;
    private List<String> readBy;
    private EventType eventType;

    public enum EventType {
        NEW_MESSAGE,
        MESSAGE_DELETED,
        MESSAGE_READ,
        TYPING
    }
}
