// package com.socialapp.notificationservice.kafka;

// import org.springframework.kafka.annotation.KafkaListener;
// import org.springframework.stereotype.Service;

// import com.socialapp.notificationservice.dto.FriendshipEventDTO;
// import com.socialapp.notificationservice.service.NotificationService;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class FriendEventListener {

//     private final NotificationService notificationService;

//     @KafkaListener(
//         topics = "friendship-events",
//         groupId = "notification-group",
//         containerFactory = "friendshipEventKafkaListenerContainerFactory"
//     )
//     public void consume(FriendshipEventDTO event) {
//         log.info("Received friendship event: {}", event);
//         notificationService.processFriendshipEvent(event);
//     }
// }
