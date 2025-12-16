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
    private ParticipantInfo otherParticipant;
    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ParticipantInfo {
        private String userId;
        private String fullName;
        private String avatarUrl;
        private boolean isOnline;
    }
}
