package com.socialapp.postservice.dto.response;

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
public class StoryResponse {
    String id;

    String authorId;
    String type;
    String content;

    List<String> media;

    Instant createdAt;
    Instant updatedAt;

    String privacy;

    List<String> likes;
    List<String> seenBy;
    Integer commentsCount;
}
