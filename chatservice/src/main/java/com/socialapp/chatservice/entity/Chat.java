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
@Document(collection = "chats")
public class Chat {
    @Id
    private String id;

    private List<String> participants; 

    private String lastMessageId;   

    private String lastMessage; 

    private Instant lastMessageTime; 

    private String lastMessageSenderId;

    private Instant createdAt;

    private Instant updatedAt;

    private List<String> readBy; 

    private List<String> deletedBy;

    public Chat(List<String> participants) {
        this.participants = participants;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.readBy = new ArrayList<>();
        this.deletedBy = new ArrayList<>();
    }
}
