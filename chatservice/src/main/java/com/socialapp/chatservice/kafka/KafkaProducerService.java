package com.socialapp.chatservice.kafka;

import com.socialapp.chatservice.dto.event.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String CHAT_MESSAGE_TOPIC = "chat-messages";
    private static final String NOTIFICATION_EVENTS_TOPIC = "notification-events";

    /**
     * Gửi event tin nhắn mới vào Kafka
     */
    public void sendChatMessage(ChatMessageEvent event) {
        try {
            log.info("Sending chat message event to Kafka: chatId={}, messageId={}",
                    event.getChatId(), event.getMessageId());

            kafkaTemplate.send(CHAT_MESSAGE_TOPIC, event.getChatId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Message sent successfully: {}", event.getMessageId());
                        } else {
                            log.error("Failed to send message to Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending message to Kafka: {}", e.getMessage(), e);
        }
    }

    /**
     * Gửi notification tới NotificationService để xử lý in-app notification và push notification
     */
    public void sendMessageNotification(com.socialapp.chatservice.dto.event.BaseEvent baseEvent) {
        try {
            log.info("Sending message notification event to NotificationService: eventType={}", 
                    baseEvent.getEventType());

            kafkaTemplate.send(NOTIFICATION_EVENTS_TOPIC, baseEvent)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Notification event sent successfully");
                        } else {
                            log.error("Failed to send notification event: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending notification event: {}", e.getMessage(), e);
        }
    }
}

