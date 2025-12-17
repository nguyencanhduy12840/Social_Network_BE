package com.socialapp.chatservice.websocket;

import com.socialapp.chatservice.dto.event.ChatMessageEvent;
import com.socialapp.chatservice.entity.Chat;
import com.socialapp.chatservice.kafka.KafkaProducerService;
import com.socialapp.chatservice.repository.ChatRepository;
import com.socialapp.chatservice.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final OnlineUserService onlineUserService;
    private final KafkaProducerService kafkaProducerService;
    private final ChatRepository chatRepository;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            String userId = user.getName();
            onlineUserService.userConnected(userId);
            broadcastOnlineStatus(userId, ChatMessageEvent.EventType.USER_ONLINE);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            String userId = user.getName();
            onlineUserService.userDisconnected(userId);
            broadcastOnlineStatus(userId, ChatMessageEvent.EventType.USER_OFFLINE);
        }
    }

    /**
     * Broadcast online status change to all chat participants
     */
    private void broadcastOnlineStatus(String userId, ChatMessageEvent.EventType eventType) {
        try {
            // Tìm tất cả conversation của user để lấy danh sách participants
            Set<String> recipientIds = new HashSet<>();
            Page<Chat> chats = chatRepository.findByParticipantsContaining(userId, PageRequest.of(0, 100));
            
            for (Chat chat : chats.getContent()) {
                for (String participantId : chat.getParticipants()) {
                    if (!participantId.equals(userId)) {
                        recipientIds.add(participantId);
                    }
                }
            }

            // Gửi event tới từng recipient
            for (String recipientId : recipientIds) {
                ChatMessageEvent statusEvent = ChatMessageEvent.builder()
                        .eventType(eventType)
                        .sender(com.socialapp.chatservice.dto.response.UserResponse.builder().id(userId).build())
                        .recipientId(recipientId)
                        .build();
                
                kafkaProducerService.sendChatMessage(statusEvent);
            }

            log.info("Broadcasted {} status for user {} to {} recipients", 
                    eventType, userId, recipientIds.size());
        } catch (Exception e) {
            log.error("Error broadcasting online status for user {}: {}", userId, e.getMessage());
        }
    }
}
