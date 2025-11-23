package com.socialapp.postservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCommentRequest {
    private String commentId;
    private String content;
    private List<String> media;
    private Instant updatedAt;
}
