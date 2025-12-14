package com.socialapp.chatservice.dto.event;

import com.socialapp.chatservice.entity.Message;
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
    private String recipientId; // Người nhận
    private String content;
    private Message.MessageType type;
    private String fileUrl;
    private String fileName;
    private Instant createdAt;
    private List<String> readBy;
    private EventType eventType;

    public enum EventType {
        NEW_MESSAGE,      // Tin nhắn mới
        MESSAGE_DELETED,  // Tin nhắn bị xóa
        MESSAGE_READ      // Tin nhắn đã đọc
    }
}

