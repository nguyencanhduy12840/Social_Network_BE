package com.socialapp.chatservice.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageResponse {
    private String id;
    private String chatId;
    private String senderId;
    private String content;
    private List<String> attachments;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isDeleted;
    private List<String> readBy;
}
