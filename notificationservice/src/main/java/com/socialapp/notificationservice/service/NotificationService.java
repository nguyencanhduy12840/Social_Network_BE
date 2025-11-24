package com.socialapp.notificationservice.service;

import java.time.Instant;

import com.socialapp.notificationservice.dto.CommentEventDTO;
import org.modelmapper.ModelMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.socialapp.notificationservice.dto.BaseEvent;
import com.socialapp.notificationservice.dto.FriendshipEventDTO;
import com.socialapp.notificationservice.dto.PostEventDTO;
import com.socialapp.notificationservice.entity.Notification;
import com.socialapp.notificationservice.repository.NotificationRepository;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ModelMapper modelMapper;

    public NotificationService(NotificationRepository notificationRepository, SimpMessagingTemplate messagingTemplate, ModelMapper modelMapper) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.modelMapper = modelMapper;
    }

    public void handleFriendshipEvent(BaseEvent event) {
        FriendshipEventDTO dto = modelMapper.map(event.getPayload(), FriendshipEventDTO.class);
        Notification notification = Notification.builder()
                .senderId(dto.getSenderId())
                .receiverId(dto.getReceiverId())
                .type(dto.getType())
                .message(dto.getMessage())
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);

        // Push realtime
        messagingTemplate.convertAndSend("/topic/notifications/" + dto.getReceiverId(), notification);
    }

    public void handlePostEvent(BaseEvent event){
        PostEventDTO eventDTO = modelMapper.map(event.getPayload(), PostEventDTO.class);
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
        messagingTemplate.convertAndSend("/topic/notifications/" + eventDTO.getReceiverId(), notification);
    }

    public void handleCommentEvent(BaseEvent event){
        CommentEventDTO eventDTO = modelMapper.map(event.getPayload(), CommentEventDTO.class);
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
        messagingTemplate.convertAndSend("/topic/notifications/" + eventDTO.getReceiverId(), notification);
    }
}
