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

    private String content;

    private List<String> attachments;

    private Instant createdAt;

    private Instant updatedAt;

    private boolean isDeleted;

    private List<String> readBy;

    private List<String> deletedBy;

    public Message(String chatId, String senderId, String content,List<String> attachments ) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = content;
        this.attachments = attachments;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.isDeleted = false;
        this.readBy = new ArrayList<>();
        this.deletedBy = new ArrayList<>();
    }
}

