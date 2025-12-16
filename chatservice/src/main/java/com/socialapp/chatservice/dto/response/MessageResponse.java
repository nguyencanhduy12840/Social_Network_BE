package com.socialapp.chatservice.dto.response;

import com.socialapp.chatservice.entity.Message;
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
    private Message.MessageType type;
    private String content;
    private String fileUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isDeleted;
    private List<String> readBy;
}
