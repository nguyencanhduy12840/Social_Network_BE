package com.socialapp.notificationservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.socialapp.notificationservice.entity.PushToken;

@Repository
public interface PushTokenRepository extends MongoRepository<PushToken, String> {
    List<PushToken> findByUserId(String userId);
    Optional<PushToken> findByToken(String token);
    void deleteByToken(String token);
}
