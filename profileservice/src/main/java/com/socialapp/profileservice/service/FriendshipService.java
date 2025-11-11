package com.socialapp.profileservice.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.socialapp.profileservice.dto.request.BaseEvent;
import com.socialapp.profileservice.dto.request.FriendActionRequest;
import com.socialapp.profileservice.dto.request.FriendshipEventDTO;
import com.socialapp.profileservice.entity.Friendship;
import com.socialapp.profileservice.entity.UserProfile;
import com.socialapp.profileservice.exception.ExistException;
import com.socialapp.profileservice.repository.UserProfileRepository;
import com.socialapp.profileservice.util.FriendshipStatus;

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

        if(senderId.equals(receiverId)) return "Cannot send friend request to yourself";

        UserProfile sender = userProfileRepository.findByUserId(senderId)
                .orElseThrow(() -> new ExistException("Sender not found"));
        UserProfile receiver = userProfileRepository.findByUserId(receiverId)
                .orElseThrow(() -> new ExistException("Receiver not found"));

        if(sender.getFriendships() == null) sender.setFriendships(new HashSet<>());

        Optional<Friendship> existing = sender.getFriendships().stream()
                .filter(f -> f.getFriend().getUserId().equals(receiverId))
                .findFirst();
        if(existing.isPresent()){
            return "Friend request already exists or you are already friends";
        }

        Friendship friendship = Friendship.builder()
                .friend(receiver)
                .status(FriendshipStatus.PENDING)
                .requestedAt(Instant.now())
                .build();

        sender.getFriendships().add(friendship);
        userProfileRepository.save(sender);

        // Push Kafka event
        
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
        String receiverId = request.getFriendUserId();
        String senderId = request.getUserId();

        UserProfile sender = userProfileRepository.findByUserId(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        UserProfile receiver = userProfileRepository.findByUserId(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Optional<Friendship> friendRequest = sender.getFriendships().stream()
                .filter(f -> f.getFriend().getUserId().equals(receiverId)
                        && f.getStatus() == FriendshipStatus.PENDING)
                .findFirst();

        if(friendRequest.isEmpty()) return "No pending friend request found";

        Friendship friendship = friendRequest.get();
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setSince(Instant.now());
        userProfileRepository.save(sender);

        // Push Kafka event
        FriendshipEventDTO event = FriendshipEventDTO.builder()
                .type("FRIEND_REQUEST_ACCEPTED")
                .senderId(senderId)
                .receiverId(receiverId)
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
    public String rejectFriendRequest(FriendActionRequest request) {
        String receiverId = request.getUserId();
        String senderId = request.getFriendUserId();

        UserProfile sender = userProfileRepository.findByUserId(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Optional<Friendship> friendRequest = sender.getFriendships().stream()
                .filter(f -> f.getFriend().getUserId().equals(receiverId)
                        && f.getStatus() == FriendshipStatus.PENDING)
                .findFirst();

        if(friendRequest.isEmpty()) return "No pending friend request found";

        Friendship friendship = friendRequest.get();
        friendship.setStatus(FriendshipStatus.REJECTED);
        userProfileRepository.save(sender);

        // Optional: push Kafka event for rejection
        FriendshipEventDTO event = FriendshipEventDTO.builder()
                .type("FRIEND_REQUEST_REJECTED")
                .senderId(senderId)
                .receiverId(receiverId)
                .message("User " + receiverId + " rejected your friend request")
                .build();
        BaseEvent wrapper = BaseEvent.builder()
        .eventType("FRIEND_REQUEST_REJECTED")
        .sourceService("ProfileService")
        .payload(event)
        .build();
        kafkaTemplate.send(NOTIFICATION_TOPIC, wrapper);

        return "Friend request rejected";
    }

    @Transactional
    public String cancelFriendRequest(FriendActionRequest request) {
        String senderId = request.getUserId();
        String receiverId = request.getFriendUserId();

        UserProfile sender = userProfileRepository.findByUserId(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Optional<Friendship> friendRequest = sender.getFriendships().stream()
                .filter(f -> f.getFriend().getUserId().equals(receiverId)
                        && f.getStatus() == FriendshipStatus.PENDING)
                .findFirst();

        if(friendRequest.isEmpty()) return "No pending friend request to cancel";

        sender.getFriendships().remove(friendRequest.get());
        userProfileRepository.save(sender);

        // Optional: push Kafka event for cancellation
        FriendshipEventDTO event = FriendshipEventDTO.builder()
                .type("FRIEND_REQUEST_CANCELLED")
                .senderId(senderId)
                .receiverId(receiverId)
                .message("User " + senderId + " cancelled the friend request")
                .build();
        BaseEvent wrapper = BaseEvent.builder()
        .eventType("FRIEND_REQUEST_CANCELLED")
        .sourceService("ProfileService")
        .payload(event)
        .build();
        kafkaTemplate.send(NOTIFICATION_TOPIC, wrapper);

        return "Friend request cancelled";
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
