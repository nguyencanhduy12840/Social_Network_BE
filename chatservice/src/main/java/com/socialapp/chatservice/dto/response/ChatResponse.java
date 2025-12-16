package com.socialapp.chatservice.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatResponse {
    private String id;
    private List<String> participants;
    private String lastMessage;
    private Instant lastMessageTime;
    private String lastMessageSenderId;
    private int unreadCount;
    private UserResponse otherParticipant;
    private Instant createdAt;
    private Instant updatedAt;
}
