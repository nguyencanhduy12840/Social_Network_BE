package com.socialapp.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialapp.notificationservice.dto.*;
import com.socialapp.notificationservice.entity.Notification;
import com.socialapp.notificationservice.entity.NotificationSettings;
import com.socialapp.notificationservice.entity.PushToken;
import com.socialapp.notificationservice.repository.NotificationRepository;
import com.socialapp.notificationservice.repository.NotificationSettingsRepository;
import com.socialapp.notificationservice.repository.PushTokenRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final PushTokenRepository pushTokenRepository;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public NotificationService(NotificationRepository notificationRepository,
            PushTokenRepository pushTokenRepository,
            NotificationSettingsRepository notificationSettingsRepository,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.pushTokenRepository = pushTokenRepository;
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public void handleFriendshipEvent(BaseEvent event) {
        FriendshipEventDTO dto = objectMapper.convertValue(event.getPayload(), FriendshipEventDTO.class);
        Notification notification = Notification.builder()
                .senderId(dto.getSenderId())
                .receiverId(dto.getReceiverId())
                .type(dto.getType())
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        // Push realtime
        messagingTemplate.convertAndSend("/topic/notifications/" + dto.getReceiverId(), notification);

        // Send Push Notification
        sendPushNotification(notification);
    }

    public void handlePostEvent(BaseEvent event) {
        PostEventDTO eventDTO = objectMapper.convertValue(event.getPayload(), PostEventDTO.class);
        Notification notification = Notification.builder()
                .senderId(eventDTO.getAuthorId())
                .receiverId(eventDTO.getReceiverId())
                .type(eventDTO.getEventType())
                .extraData(Notification.ExtraData.builder()
                        .postId(eventDTO.getPostId())
                        .storyId(eventDTO.getStoryId())
                        .groupId(eventDTO.getGroupId())
                        .build())
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        // Push realtime
        messagingTemplate.convertAndSend("/topic/notifications/" + eventDTO.getReceiverId(), notification);

        // Send Push Notification
        sendPushNotification(notification);
    }

    public void handleCommentEvent(BaseEvent event) {
        CommentEventDTO eventDTO = objectMapper.convertValue(event.getPayload(), CommentEventDTO.class);
        Notification notification = Notification.builder()
                .senderId(eventDTO.getAuthorId())
                .receiverId(eventDTO.getReceiverId())
                .type(eventDTO.getEventType())
                .extraData(Notification.ExtraData.builder()
                        .commentId(eventDTO.getCommentId())
                        .postId(eventDTO.getPostId())
                        .storyId(eventDTO.getStoryId())
                        .groupId(eventDTO.getGroupId())
                        .build())
                .isRead(false)
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(notification);
        // Push realtime
        messagingTemplate.convertAndSend("/topic/notifications/" + eventDTO.getReceiverId(), notification);

        // Send Push Notification
        sendPushNotification(notification);
    }

    public void handleChatEvent(BaseEvent event) {
        ChatMessageEventDTO eventDTO = objectMapper.convertValue(event.getPayload(), ChatMessageEventDTO.class);
        Notification notification = Notification.builder()
                .senderId(eventDTO.getSenderId())
                .receiverId(eventDTO.getReceiverId())
                .type(eventDTO.getEventType())
                .extraData(Notification.ExtraData.builder()
                        .chatId(eventDTO.getChatId())
                        .messageId(eventDTO.getMessageId())
                        .build())
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        // Push realtime via WebSocket
        messagingTemplate.convertAndSend("/topic/notifications/" + eventDTO.getReceiverId(), notification);

        // Send Push Notification
        sendPushNotification(notification);
    }

    public void handleGroupEvent(BaseEvent event) {
        GroupEventDTO eventDTO = objectMapper.convertValue(event.getPayload(), GroupEventDTO.class);
        Notification notification = Notification.builder()
                .senderId(eventDTO.getSenderId())
                .receiverId(eventDTO.getReceiverId())
                .type(eventDTO.getType())
                .extraData(Notification.ExtraData.builder()
                        .groupId(eventDTO.getGroupId())
                        .build())
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        // Push realtime via WebSocket
        messagingTemplate.convertAndSend("/topic/notifications/" + eventDTO.getReceiverId(), notification);

        // Send Push Notification
        sendPushNotification(notification);
    }

    // ==========================================
    // Notification Management APIs
    // ==========================================

    public List<Notification> getNotifications(String userId) {
        return notificationRepository.findByReceiverIdAndTypeNotOrderByCreatedAtDesc(userId, "NEW_MESSAGE");
    }

    public Page<Notification> getNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByReceiverIdAndTypeNotOrderByCreatedAtDesc(userId, "NEW_MESSAGE", pageable);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    public void markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Security check: only owner can mark as read
        if (!notification.getReceiverId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only mark your own notifications as read");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllAsRead(String userId) {
        List<Notification> unreadNotifications = notificationRepository.findByReceiverIdAndIsReadFalse(userId);
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    public void deleteNotification(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Security check: only owner can delete
        if (!notification.getReceiverId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only delete your own notifications");
        }

        notificationRepository.delete(notification);
    }

    // ==========================================
    // Push Notification Management
    // ==========================================

    public void registerPushToken(RegisterPushTokenRequest request, String userId) {
        Optional<PushToken> existingToken = pushTokenRepository.findByToken(request.getToken());
        if (existingToken.isPresent()) {
            PushToken token = existingToken.get();
            if (!token.getUserId().equals(userId)) {
                token.setUserId(userId);
                token.setUpdatedAt(Instant.now());
                pushTokenRepository.save(token);
            }
        } else {
            PushToken newToken = PushToken.builder()
                    .userId(userId)
                    .token(request.getToken())
                    .deviceType(request.getType())
                    .deviceId(request.getDeviceId())
                    .deviceName(request.getDeviceName())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            pushTokenRepository.save(newToken);
        }
    }

    public void unregisterPushToken(UnregisterPushTokenRequest request) {
        pushTokenRepository.deleteByToken(request.getToken());
    }

    public NotificationSettings getSettings(String userId) {
        return notificationSettingsRepository.findByUserId(userId)
                .orElse(NotificationSettings.builder()
                        .userId(userId)
                        .enabled(true)
                        .settings(new HashMap<>())
                        .build());
    }

    public void updateSettings(UpdateNotificationSettingsRequest request, String userId) {
        NotificationSettings settings = notificationSettingsRepository.findByUserId(userId)
                .orElse(NotificationSettings.builder()
                        .userId(userId)
                        .enabled(true)
                        .settings(new HashMap<>())
                        .createdAt(Instant.now())
                        .build());

        if (request.getEnabled() != null) {
            settings.setEnabled(request.getEnabled());
        }
        if (request.getSettings() != null) {
            if (settings.getSettings() == null) {
                settings.setSettings(new HashMap<>());
            }
            settings.getSettings().putAll(request.getSettings());
        }
        settings.setUpdatedAt(Instant.now());
        notificationSettingsRepository.save(settings);
    }

    private void sendPushNotification(Notification notification) {
        try {
            // 1. Check settings
            NotificationSettings settings = notificationSettingsRepository.findByUserId(notification.getReceiverId())
                    .orElse(NotificationSettings.builder().enabled(true).settings(new HashMap<>()).build());

            if (!settings.isEnabled()) {
                return;
            }

            if (settings.getSettings() != null
                    && Boolean.FALSE.equals(settings.getSettings().get(notification.getType()))) {
                return;
            }

            // 2. Get tokens
            List<PushToken> tokens = pushTokenRepository.findByUserId(notification.getReceiverId());
            if (tokens.isEmpty()) {
                return;
            }

            // 3. Prepare Payload
            String title = "Social App";
            String body = getNotificationBody(notification);

            List<Map<String, Object>> payload = new ArrayList<>();
            for (PushToken token : tokens) {
                Map<String, Object> message = new HashMap<>();
                message.put("to", token.getToken());
                message.put("title", title);
                message.put("body", body);
                message.put("data", notification);
                message.put("sound", "default");
                payload.add(message);
            }

            // 4. Send to Expo
            String url = "https://exp.host/--/api/v2/push/send";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            System.err.println("Error sending push notification: " + e.getMessage());
        }
    }

    private String getNotificationBody(Notification notification) {
        switch (notification.getType()) {
            case "NEW_POST":
                return "Someone posted a new photo";
            case "NEW_STORY":
                return "Someone posted a new story";
            case "LIKE_POST":
                return "Someone liked your post";
            case "LIKE_STORY":
                return "Someone liked your story";
            case "COMMENT_ON_POST":
                return "Someone commented on your post";
            case "COMMENT_ON_STORY":
                return "Someone commented on your story";
            case "REPLY_COMMENT":
                return "Someone replied to your comment";
            case "LIKE_COMMENT":
                return "Someone liked your comment";
            case "FRIEND_REQUEST":
                return "Someone sent you a friend request";
            case "FRIEND_REQUEST_ACCEPTED":
                return "Someone accepted your friend request";
            case "GROUP_JOIN_REQUEST":
                return "Someone requested to join your group";
            case "GROUP_JOIN_ACCEPTED":
                return "Someone accepted your group join request";
            case "GROUP_ROLE_CHANGE":
                return "Your role in the group has changed";
            case "GROUP_NEW_POST":
                return "Someone posted in the group";
            case "NEW_MESSAGE":
                return "You have a new message";
            default:
                return "You have a new notification";
        }
    }
}
