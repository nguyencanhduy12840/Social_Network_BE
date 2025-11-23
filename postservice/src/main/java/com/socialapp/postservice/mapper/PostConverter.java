package com.socialapp.postservice.mapper;

import com.socialapp.postservice.dto.response.PostResponse;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.socialapp.postservice.dto.response.CreatePostResponse;
import com.socialapp.postservice.entity.Post;

@Component
public class PostConverter {
    private final ModelMapper modelMapper;

    public PostConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public CreatePostResponse convertToCreatePostResponse(Post post) {
        CreatePostResponse createPostResponse = modelMapper.map(post, CreatePostResponse.class);
        return createPostResponse;
    }

    public PostResponse convertToPostResponse(Post post) {
        PostResponse postResponse = modelMapper.map(post, PostResponse.class);
        return postResponse;
    }
}
