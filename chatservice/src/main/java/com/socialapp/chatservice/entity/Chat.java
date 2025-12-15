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

    private List<String> participants; // Danh sách 2 userId tham gia chat

    private String lastMessageId; // ID tin nhắn cuối cùng

    private String lastMessage; // Nội dung tin nhắn cuối

    private Instant lastMessageTime; // Thời gian tin nhắn cuối

    private String lastMessageSenderId; // Người gửi tin nhắn cuối

    private Instant createdAt;

    private Instant updatedAt;

    // Theo dõi ai đã đọc tin nhắn cuối
    private List<String> readBy; // Danh sách userId đã đọc

    public Chat(List<String> participants) {
        this.participants = participants;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.readBy = new ArrayList<>();
    }
}
