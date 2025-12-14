package com.socialapp.chatservice.websocket;

import com.socialapp.chatservice.dto.request.SendMessageRequest;
import com.socialapp.chatservice.dto.response.MessageResponse;
import com.socialapp.chatservice.service.ChatService;
import com.socialapp.chatservice.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final ChatService chatService;

    /**
     * Client gửi tin nhắn qua WebSocket
     * Endpoint: /app/chat.sendMessage
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(
            @Payload SendMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        try {
            // Lấy userId từ principal (đã authenticated)
            String currentUserId = principal.getName();

            log.info("Received message from user {} via WebSocket", currentUserId);

            // Xử lý tin nhắn (sẽ tự động gửi qua Kafka và WebSocket)
            MessageResponse message = chatService.sendMessage(currentUserId, request);

            log.info("Message processed successfully: {}", message.getId());

        } catch (Exception e) {
            log.error("Error processing WebSocket message: {}", e.getMessage(), e);
        }
    }

    /**
     * Client thông báo đang typing
     * Endpoint: /app/chat.typing
     */
    @MessageMapping("/chat.typing")
    public void userTyping(
            @Payload TypingEvent event,
            Principal principal) {
        try {
            String currentUserId = principal.getName();
            log.info("User {} is typing in chat {}", currentUserId, event.getChatId());

            // Có thể broadcast typing indicator tới người khác trong conversation

        } catch (Exception e) {
            log.error("Error processing typing event: {}", e.getMessage(), e);
        }
    }

    @lombok.Data
    public static class TypingEvent {
        private String chatId;
        private boolean isTyping;
    }
}

