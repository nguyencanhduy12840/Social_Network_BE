package com.socialapp.postservice.mapper;

import com.socialapp.postservice.dto.request.CreateCommentRequest;
import com.socialapp.postservice.entity.Comment;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CommentConverter {
    private final ModelMapper modelMapper;
    public CommentConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public Comment toComment(CreateCommentRequest createCommentRequest) {
        Comment comment = new Comment();
        comment.setPostId(createCommentRequest.getPostId());
        comment.setParentCommentId(createCommentRequest.getParentCommentId());
        comment.setAuthorId(createCommentRequest.getAuthorId());
        comment.setContent(createCommentRequest.getContent());
        comment.setCreatedAt(Instant.now());
        return comment;
    }
}
