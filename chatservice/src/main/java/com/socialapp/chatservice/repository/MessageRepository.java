package com.socialapp.chatservice.repository;

import com.socialapp.chatservice.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    // Lấy tất cả tin nhắn trong 1 conversation với phân trang (trừ những tin đã bị user xóa soft)
    @Query("{ 'chatId': ?0, 'isDeleted': false, 'deletedBy': { $ne: ?1 } }")
    Page<Message> findByChatIdAndIsDeletedFalseAndDeletedByNotContains(String chatId, String userId, Pageable pageable);

    // Đếm số tin nhắn chưa đọc trong conversation (trừ tin đã xóa soft)
    @Query(value = "{ 'chatId': ?0, 'senderId': { $ne: ?1 }, 'readBy': { $ne: ?1 }, 'deletedBy': { $ne: ?1 } }", count = true)
    long countUnreadMessages(String chatId, String userId);

    // Lấy tin nhắn cuối cùng trong conversation
    Message findFirstByChatIdAndIsDeletedFalseOrderByCreatedAtDesc(String chatId);

    // Lấy danh sách tin nhắn chưa đọc
    @Query("{ 'chatId': ?0, 'senderId': { $ne: ?1 }, 'readBy': { $ne: ?1 } }")
    List<Message> findUnreadMessages(String chatId, String userId);

    @Query(value = "{ 'senderId': { $ne: ?0 }, 'readBy': { $ne: ?0 }, 'deletedBy': { $ne: ?0 } }", count = true)
    long countAllUnreadMessages(String userId);

    void deleteAllByChatId(String chatId);

    List<Message> findAllByChatId(String chatId);

    // Lấy tin nhắn cuối cùng chưa bị user xóa
    Message findFirstByChatIdAndIsDeletedFalseAndDeletedByNotContainingOrderByCreatedAtDesc(String chatId, String userId);
}
