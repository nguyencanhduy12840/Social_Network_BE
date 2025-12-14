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
    private String content;
    private Message.MessageType type;
    private String fileUrl;
    private String fileName;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isDeleted;
    private List<String> readBy;
    private boolean isMine; // Đánh dấu tin nhắn của mình hay của người khác
    private SenderInfo senderInfo; // Thông tin người gửi

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class SenderInfo {
        private String userId;
        private String fullName;
        private String avatarUrl;
    }
}

