package com.socialapp.notificationservice.service;

import java.time.Instant;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.socialapp.notificationservice.dto.FriendshipEventDTO;
import com.socialapp.notificationservice.dto.PostEventDTO;
import com.socialapp.notificationservice.entity.Notification;
import com.socialapp.notificationservice.repository.NotificationRepository;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    public NotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

     public void processFriendshipEvent(FriendshipEventDTO event) {
        Notification notification = Notification.builder()
                .senderId(event.getSenderId())
                .receiverId(event.getReceiverId())
                .type(event.getType())
                .message(event.getMessage())
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        // Push realtime
        messagingTemplate.convertAndSend("/topic/notifications/" + event.getReceiverId(), notification);
    }

    public void processPostEvent(PostEventDTO eventDTO){
        Notification notification = Notification.builder()
                .senderId(eventDTO.getAuthorId())
                .receiverId(eventDTO.getReceiverId())
                .type(eventDTO.getEventType())
                .message(eventDTO.getContent())
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        // Push realtime
        messagingTemplate.convertAndSend("/topic/posts/" + eventDTO.getReceiverId(), notification);
    }
}
