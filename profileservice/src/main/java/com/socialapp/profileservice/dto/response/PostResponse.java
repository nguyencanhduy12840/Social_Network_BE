package com.socialapp.profileservice.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PostResponse {
    List<PostOne> data;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PostOne {
        String id;

    String authorId;
    String groupId;
    String type;
    String content;

    List<String> media;

    Instant createdAt;
    Instant updatedAt;

    String privacy;

    List<String> likes;
    Integer commentsCount;
    }
}
