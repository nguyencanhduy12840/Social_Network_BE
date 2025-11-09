package com.socialapp.notificationservice.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.socialapp.notificationservice.entity.Notification;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByReceiverIdOrderByCreatedAtDesc(String receiverId);
}
