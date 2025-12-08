package com.socialapp.notificationservice.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.socialapp.notificationservice.entity.NotificationSettings;

@Repository
public interface NotificationSettingsRepository extends MongoRepository<NotificationSettings, String> {
    Optional<NotificationSettings> findByUserId(String userId);
}
