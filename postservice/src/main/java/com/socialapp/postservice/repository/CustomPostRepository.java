package com.socialapp.postservice.repository;

import com.socialapp.postservice.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomPostRepository {
    Page<Post> findPostsForMainScreen(String currentUserId, List<String> friendIds, Pageable pageable);
}

