package com.socialapp.postservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.socialapp.postservice.entity.Post;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    
}
