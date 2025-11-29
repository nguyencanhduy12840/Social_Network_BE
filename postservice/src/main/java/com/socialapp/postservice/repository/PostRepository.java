package com.socialapp.postservice.repository;

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

    List<Post> findByAuthorIdAndPrivacyIn(String authorId, List<String> privacy);

    List<Post> findByAuthorId(String authorId);

    List<Post> findByAuthorIdInAndPrivacyInOrderByCreatedAtDesc(List<String> authorIds, List<String> privacy);

    List<Post> findByPrivacyOrderByCreatedAtDesc(String privacy);

    // Thêm các phương thức với Pageable cho pagination
    Page<Post> findByPrivacyOrderByCreatedAtDesc(String privacy, Pageable pageable);

    Page<Post> findByAuthorIdInAndPrivacyInOrderByCreatedAtDesc(List<String> authorIds, List<String> privacy, Pageable pageable);

    Page<Post> findByAuthorIdOrPrivacyOrderByCreatedAtDesc(String authorId, String privacy, Pageable pageable);
}
