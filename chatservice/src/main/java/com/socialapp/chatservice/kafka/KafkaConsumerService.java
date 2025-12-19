package com.socialapp.chatservice.kafka;

import com.socialapp.chatservice.dto.event.ChatMessageEvent;
import com.socialapp.chatservice.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final WebSocketService webSocketService;

    /**
     * Lắng nghe tin nhắn từ Kafka và push qua WebSocket tới client
     */
    @KafkaListener(topics = "chat-messages", groupId = "chat-service-group")
    public void consumeChatMessage(ChatMessageEvent event) {
        try {
            log.info("Processing {} event for chat: {}", event.getEventType(), event.getChatId());

            // Handle typing events
            if (event.getEventType() == ChatMessageEvent.EventType.TYPING) {
                if (event.getRecipientId() != null) {
                    webSocketService.sendTypingEvent(event.getRecipientId(), event);
                }
                return;
            }

            // Handle online status events
            if (event.getEventType() == ChatMessageEvent.EventType.USER_ONLINE ||
                event.getEventType() == ChatMessageEvent.EventType.USER_OFFLINE) {
                if (event.getRecipientId() != null) {
                    webSocketService.sendOnlineStatusToUser(event.getRecipientId(), event);
                }
                return;
            }

            // Push message to recipient via WebSocket
            if (event.getRecipientId() != null) {
                webSocketService.sendMessageToUser(event.getRecipientId(), event);
            }

            // Push message to sender (for multi-device sync)
            if (event.getSender() != null && event.getSender().getId() != null) {
                webSocketService.sendMessageToUser(event.getSender().getId(), event);
            }

        } catch (Exception e) {
            log.error("Failed to process chat event: {}", e.getMessage(), e);
        }
    }
}
