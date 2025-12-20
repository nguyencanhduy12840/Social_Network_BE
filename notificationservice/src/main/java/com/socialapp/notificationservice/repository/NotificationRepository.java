package com.socialapp.notificationservice.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.socialapp.notificationservice.entity.Notification;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByReceiverIdOrderByCreatedAtDesc(String receiverId);

    Page<Notification> findByReceiverIdOrderByCreatedAtDesc(String receiverId, Pageable pageable);

    List<Notification> findByReceiverIdAndTypeNotOrderByCreatedAtDesc(String receiverId, String type);

    Page<Notification> findByReceiverIdAndTypeNotOrderByCreatedAtDesc(String receiverId, String type,
            Pageable pageable);

    long countByReceiverIdAndIsReadFalse(String receiverId);

    List<Notification> findByReceiverIdAndIsReadFalse(String receiverId);
}
