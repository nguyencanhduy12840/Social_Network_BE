package com.socialapp.chatservice.dto.event;

import com.socialapp.chatservice.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event gửi tới NotificationService qua Kafka topic "notification-events"
 * Phù hợp với format BaseEvent của NotificationService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventType;      // e.g., "NEW_CHAT_MESSAGE"
    private String sourceService;  // "ChatService"
    private NotificationPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPayload {
        private UserResponse sender;
        private String receiverId;
        private String chatId;
        private String messageId;
        private String content;
    }
}
