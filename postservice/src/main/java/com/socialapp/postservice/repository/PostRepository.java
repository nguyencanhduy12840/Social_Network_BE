package com.socialapp.postservice.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.socialapp.postservice.entity.Post;

@Repository
public interface PostRepository extends MongoRepository<Post, String>, CustomPostRepository {
    Optional<Post> findById(String postId);

    List<Post> findByAuthorIdAndPrivacyIn(String authorId, Collection<String> privacies);

    List<Post> findByAuthorId(String authorId);

    List<Post> findByAuthorIdInAndPrivacyInOrderByCreatedAtDesc(List<String> authorIds, List<String> privacy);

    List<Post> findByPrivacyOrderByCreatedAtDesc(String privacy);

    // Thêm các phương thức với Pageable cho pagination
    Page<Post> findByPrivacyAndTypeOrderByCreatedAtDesc(String privacy, String type, Pageable pageable);

    Page<Post> findByAuthorIdInAndPrivacyInOrderByCreatedAtDesc(List<String> authorIds, List<String> privacy,
            Pageable pageable);

    Page<Post> findByAuthorIdOrPrivacyOrderByCreatedAtDesc(String authorId, String privacy, Pageable pageable);

    Page<Post> findAllByGroupIdOrderByCreatedAtDesc(String groupId, Pageable pageable);

    Page<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);

    Page<Post> findByAuthorIdAndPrivacyInOrderByCreatedAtDesc(String authorId, List<String> privacy, Pageable pageable);

    Integer countByAuthorId(String authorId);

    Page<Post> findByAuthorIdAndTypeOrderByCreatedAtDesc(String authorId, String type, Pageable pageable);

    Page<Post> findByAuthorIdAndTypeAndPrivacyInOrderByCreatedAtDesc(String authorId, String type,
            Collection<String> privacies, Pageable pageable);

    Page<Post> findByAuthorIdAndTypeAndGroupIdIsNullOrderByCreatedAtDesc(String authorId, String type,
            Pageable pageable);

    Page<Post> findByAuthorIdAndTypeAndPrivacyInAndGroupIdIsNullOrderByCreatedAtDesc(String authorId, String type,
            Collection<String> privacies, Pageable pageable);

    // Delete methods for group operations
    void deleteAllByGroupId(String groupId);

    void deleteAllByGroupIdAndAuthorId(String groupId, String authorId);

    // Search posts by content (type POST, group null)
    List<Post> findByTypeAndGroupIdIsNullAndContentContainingIgnoreCaseOrderByCreatedAtDesc(
            String type, String content);
}
