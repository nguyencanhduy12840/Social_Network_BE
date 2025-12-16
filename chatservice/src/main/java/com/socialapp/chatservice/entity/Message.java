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

    private String chatId;

    private String senderId;

    private MessageType type;
    
    private String content;

    private String fileUrl;

    private Instant createdAt;

    private Instant updatedAt;

    private boolean isDeleted;

    private List<String> readBy;

    private List<String> deletedBy;

    public enum MessageType {
        TEXT, IMAGE, FILE, VIDEO, AUDIO
    }

    public Message(String chatId, String senderId,MessageType type, String content,String fileUrl ) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.type = type;
        this.content = content;
        this.fileUrl = fileUrl;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isDeleted = false;
        this.readBy = new ArrayList<>();
        this.deletedBy = new ArrayList<>();
    }
}

