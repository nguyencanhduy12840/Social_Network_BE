package com.socialapp.chatservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "messages")
public class Message {
    @Id
    private String id;

    private String chatId; // ID của conversation

    private String senderId; // ID người gửi

    private String content; // Nội dung tin nhắn

    private MessageType type; // Loại tin nhắn: TEXT, IMAGE, FILE

    private String fileUrl; // URL file nếu có

    private String fileName; // Tên file nếu có

    private Instant createdAt;

    private Instant updatedAt;

    private boolean isDeleted; // Đánh dấu tin nhắn đã xóa

    private List<String> readBy; // Danh sách userId đã đọc tin nhắn này

    public enum MessageType {
        TEXT, IMAGE, FILE, VIDEO, AUDIO
    }

    public Message(String chatId, String senderId, String content, MessageType type) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = content;
        this.type = type;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isDeleted = false;
        this.readBy = new ArrayList<>();
    }
}

