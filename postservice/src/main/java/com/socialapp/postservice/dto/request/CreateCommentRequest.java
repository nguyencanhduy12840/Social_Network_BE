package com.socialapp.postservice.dto.request;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateCommentRequest {
    private String postId;
    private String authorId;
    private String parentCommentId;
    private String content;
    private Instant createdAt;
}
