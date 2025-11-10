package com.socialapp.notificationservice.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.socialapp.notificationservice.dto.BaseEvent;
import com.socialapp.notificationservice.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventListener {
    private final NotificationService notificationService;

    @KafkaListener(topics = "notification-events", groupId = "notification-group")
    public void consume(BaseEvent baseEvent) {
    log.info("Received event: {}", baseEvent);

    switch (baseEvent.getSourceService()) {
        case "ProfileService" -> notificationService.handleFriendshipEvent(baseEvent);
        // case "PostService" -> notificationService.processPostEvent(baseEvent);
        default -> log.warn("Unknown source: {}", baseEvent.getSourceService());
    }
}
}
