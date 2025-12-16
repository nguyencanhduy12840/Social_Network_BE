package com.socialapp.chatservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    /**
     * Lắng nghe tin nhắn từ Kafka và push qua WebSocket tới client
     */
    @KafkaListener(topics = "chat-messages", groupId = "chat-service-group")
    public void consumeChatMessage(String message) {
        try {
            log.info("Received message from Kafka: {}", message);

            ChatMessageEvent event = objectMapper.readValue(message, ChatMessageEvent.class);

            if (event.getEventType() == ChatMessageEvent.EventType.TYPING) {
                if (event.getRecipientId() != null) {
                    webSocketService.sendTypingEvent(event.getRecipientId(), event);
                }
                return; // Done
            }

            // Push tin nhắn tới người nhận qua WebSocket
            if (event.getRecipientId() != null) {
                webSocketService.sendMessageToUser(event.getRecipientId(), event);
            }

            // Push tin nhắn tới người gửi (để sync giữa các devices)
            if (event.getSenderId() != null) {
                webSocketService.sendMessageToUser(event.getSenderId(), event);
            }

        } catch (Exception e) {
            log.error("Error processing message from Kafka: {}", e.getMessage(), e);
        }
    }

    /**
     * Lắng nghe notification từ Kafka
     */
    @KafkaListener(topics = "chat-notifications", groupId = "chat-service-group")
    public void consumeChatNotification(String message) {
        try {
            log.info("Received notification from Kafka: {}", message);

            ChatMessageEvent event = objectMapper.readValue(message, ChatMessageEvent.class);

            // Gửi notification tới notification service (nếu cần)
            // hoặc push notification tới mobile device

        } catch (Exception e) {
            log.error("Error processing notification from Kafka: {}", e.getMessage(), e);
        }
    }
}

