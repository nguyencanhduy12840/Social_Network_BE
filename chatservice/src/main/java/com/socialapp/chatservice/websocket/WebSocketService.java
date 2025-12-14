package com.socialapp.chatservice.websocket;

import com.socialapp.chatservice.dto.event.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi tin nhắn tới một user cụ thể qua WebSocket
     */
    public void sendMessageToUser(String userId, ChatMessageEvent event) {
        try {
            log.info("Sending message to user {} via WebSocket", userId);

            // Gửi tới destination: /user/{userId}/queue/messages
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/messages",
                    event
            );

            log.info("Message sent successfully to user {}", userId);
        } catch (Exception e) {
            log.error("Error sending message to user {} via WebSocket: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Gửi notification về conversation được cập nhật
     */
    public void sendConversationUpdate(String userId, Object data) {
        try {
            log.info("Sending conversation update to user {} via WebSocket", userId);

            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/conversations",
                    data
            );

        } catch (Exception e) {
            log.error("Error sending conversation update: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast tin nhắn tới một topic (cho group chat sau này)
     */
    public void broadcastMessage(String topic, Object message) {
        try {
            log.info("Broadcasting message to topic: {}", topic);
            messagingTemplate.convertAndSend("/topic/" + topic, message);
        } catch (Exception e) {
            log.error("Error broadcasting message: {}", e.getMessage(), e);
        }
    }
}

