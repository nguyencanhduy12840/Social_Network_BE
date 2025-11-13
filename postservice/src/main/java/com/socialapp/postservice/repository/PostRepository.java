package com.socialapp.postservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.socialapp.postservice.entity.Post;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    Optional<Post> findById(String postId);

    List<Post> findByAuthorIdAndPrivacyIn(String authorId, List<String> privacy);

    List<Post> findByAuthorId(String authorId);
}
