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

    private EventType eventType;
    private String chatId;
    private String messageId;
    private String senderId;
    private String recipientId;
    private String content;
    private List<String> attachments;
    private Instant createdAt;
    private List<String> readBy;

    public enum EventType {
        TYPING,
        USER_ONLINE,
        USER_OFFLINE,
        NEW_MESSAGE,
        MESSAGE_READ,
    }
}
