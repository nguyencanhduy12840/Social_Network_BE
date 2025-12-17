package com.socialapp.chatservice.kafka;

import com.socialapp.chatservice.dto.event.ChatMessageEvent;
import com.socialapp.chatservice.dto.event.NotificationEvent;
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
    private static final String CHAT_NOTIFICATION_TOPIC = "chat-notifications";
    private static final String NOTIFICATION_EVENTS_TOPIC = "notification-events"; // Topic của NotificationService

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
     * Gửi notification về tin nhắn mới (internal topic)
     */
    public void sendChatNotification(ChatMessageEvent event) {
        try {
            log.info("Sending chat notification to Kafka: recipientId={}", event.getRecipientId());

            kafkaTemplate.send(CHAT_NOTIFICATION_TOPIC, event.getRecipientId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Notification sent successfully to: {}", event.getRecipientId());
                        } else {
                            log.error("Failed to send notification to Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending notification to Kafka: {}", e.getMessage(), e);
        }
    }

    /**
     * Gửi notification tới NotificationService để xử lý in-app notification và push notification
     */
    public void sendToNotificationService(NotificationEvent event) {
        try {
            log.info("Sending notification to NotificationService: receiverId={}", 
                    event.getPayload() != null ? event.getPayload().getReceiverId() : "null");

            kafkaTemplate.send(NOTIFICATION_EVENTS_TOPIC, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Notification sent to NotificationService successfully");
                        } else {
                            log.error("Failed to send to NotificationService: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending to NotificationService: {}", e.getMessage(), e);
        }
    }
}

