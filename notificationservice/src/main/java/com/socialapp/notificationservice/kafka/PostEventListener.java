package com.socialapp.notificationservice.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


import com.socialapp.notificationservice.dto.PostEventDTO;
import com.socialapp.notificationservice.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PostEventListener {
     private final NotificationService notificationService;
    public PostEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "post-events", groupId = "notification-group")
    public void consume(PostEventDTO event) {
        log.info("Received post event: {}", event);
        notificationService.processPostEvent(event);
    }
}
