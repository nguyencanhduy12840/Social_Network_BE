package com.socialapp.postservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostEvent {
    String postId;
    String authorId;
    String content;
    String eventType;
    String receiverId;
}
