package com.socialapp.profileservice.service;


import java.util.List;


import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.socialapp.profileservice.dto.request.BaseEvent;
import com.socialapp.profileservice.dto.request.FriendActionRequest;
import com.socialapp.profileservice.dto.request.FriendshipEventDTO;

import com.socialapp.profileservice.entity.UserProfile;

import com.socialapp.profileservice.repository.UserProfileRepository;


import jakarta.transaction.Transactional;

@Service
public class FriendshipService {
     private final UserProfileRepository userProfileRepository;
    private final KafkaTemplate<String, BaseEvent> kafkaTemplate;

    private final String NOTIFICATION_TOPIC = "notification-events";

    public FriendshipService(UserProfileRepository userProfileRepository, KafkaTemplate<String, BaseEvent> kafkaTemplate) {
        this.userProfileRepository = userProfileRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public String sendFriendRequest(FriendActionRequest request) {
        String senderId = request.getUserId();
        String receiverId = request.getFriendUserId();

        if (senderId.equals(receiverId))
            return "Cannot send friend request to yourself";

        UserProfile sender = userProfileRepository.findByUserId(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        UserProfile receiver = userProfileRepository.findByUserId(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (userProfileRepository.hasFriendshipBetween(senderId, receiverId))
            return "Friend request already exists or you are already friends";

        userProfileRepository.createFriendRequest(senderId, receiverId);

        // Gửi Kafka event
        FriendshipEventDTO event = FriendshipEventDTO.builder()
                .type("FRIEND_REQUEST")
                .senderId(senderId)
                .receiverId(receiverId)
                .message("User " + senderId + " sent you a friend request")
                .build();

        BaseEvent wrapper = BaseEvent.builder()
                .eventType("FRIEND_REQUEST")
                .sourceService("ProfileService")
                .payload(event)
                .build();

        kafkaTemplate.send(NOTIFICATION_TOPIC, wrapper);

        return "Friend request sent";
    }

    @Transactional
    public String acceptFriendRequest(FriendActionRequest request) {
        String receiverId = request.getUserId();
        String senderId = request.getFriendUserId();

        userProfileRepository.acceptFriendship(senderId, receiverId);

        FriendshipEventDTO event = FriendshipEventDTO.builder()
                .type("FRIEND_REQUEST_ACCEPTED")
                .senderId(receiverId)
                .receiverId(senderId)
                .message("User " + receiverId + " accepted your friend request")
                .build();

        BaseEvent wrapper = BaseEvent.builder()
                .eventType("FRIEND_REQUEST_ACCEPTED")
                .sourceService("ProfileService")
                .payload(event)
                .build();

        kafkaTemplate.send(NOTIFICATION_TOPIC, wrapper);
        return "Friend request accepted";
    }

    @Transactional
    public String rejectOrCancelRequest(FriendActionRequest request) {
        userProfileRepository.deleteFriendshipBetween(
                request.getUserId(), request.getFriendUserId());

        FriendshipEventDTO event = FriendshipEventDTO.builder()
                .type("FRIEND_REQUEST_REMOVED")
                .senderId(request.getUserId())
                .receiverId(request.getFriendUserId())
                .message("Friend request removed between users")
                .build();

        BaseEvent wrapper = BaseEvent.builder()
                .eventType("FRIEND_REQUEST_REMOVED")
                .sourceService("ProfileService")
                .payload(event)
                .build();

        kafkaTemplate.send(NOTIFICATION_TOPIC, wrapper);
        return "Friend request removed";
    }

    @Transactional
    public List<UserProfile> getFriends(String userId, int page, int size) {
        long skip = (long) page * size;
        return userProfileRepository.findFriendsByUserId(userId, skip, size);
    }

    @Transactional
    public List<UserProfile> getSentRequests(String userId, int page, int size) {
        long skip = (long) page * size;
        return userProfileRepository.findSentFriendRequests(userId, skip, size);
    }

    @Transactional
        public List<UserProfile> getPendingRequests(String userId, int page, int size) {
                long skip = (long) page * size;
                return userProfileRepository.findReceivedFriendRequests(userId, skip, size);
        }
}
