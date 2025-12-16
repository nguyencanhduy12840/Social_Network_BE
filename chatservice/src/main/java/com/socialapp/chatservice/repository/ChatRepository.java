package com.socialapp.chatservice.repository;

import com.socialapp.chatservice.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends MongoRepository<Chat, String> {

    // Tìm conversation giữa 2 người (chính xác 2 người và chứa cả 2 userId)
    @Query("{ 'participants': { $all: [?0, ?1], $size: 2 } }")
    Optional<Chat> findByParticipants(String userId1, String userId2);

    // Lấy tất cả conversation của 1 user
    @Query("{ 'participants': ?0 }")
    Page<Chat> findByParticipantsContaining(String userId, Pageable pageable);

    // Tìm chat chứa participant và chưa bị xóa soft
    @Query("{ 'participants': ?0, 'deletedBy': { $ne: ?1 } }")
    Page<Chat> findByParticipantsContainingAndDeletedByNotContaining(String userId, String userIdDeleted, Pageable pageable);
}
