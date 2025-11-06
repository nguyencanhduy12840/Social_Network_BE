package com.socialapp.postservice.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.socialapp.postservice.dto.request.CreatePostRequest;
import com.socialapp.postservice.dto.response.CreatePostResponse;
import com.socialapp.postservice.entity.Post;
import com.socialapp.postservice.mapper.PostConverter;
import com.socialapp.postservice.repository.PostRepository;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final PostConverter postConverter;

    public PostService(PostRepository postRepository, PostConverter postConverter) {
        this.postRepository = postRepository;
        this.postConverter = postConverter;
    }

    public CreatePostResponse createPost( CreatePostRequest createPostRequest) {
        Post post = Post.builder().
        authorId(createPostRequest.getUserId())
        .content(createPostRequest.getContent())
        .media(createPostRequest.getMedia())
        .groupId(createPostRequest.getGroupId())
        .privacy(createPostRequest.getPrivacy())
        .commentsCount(0)
        .likes(null)
        .createdAt(Instant.now())
        .build();
        return postConverter.convertToCreatePostResponse(postRepository.save(post));
    }
}
