package com.socialapp.postservice.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document(collection = "posts")
public class Post {

    @Id
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
