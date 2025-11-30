package com.socialapp.postservice.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatePostResponse {
    String id;

    String authorId;
    String groupId;
    String type;
    String content;
    String privacy;
    List<String> media;
    Instant createdAt;
}
