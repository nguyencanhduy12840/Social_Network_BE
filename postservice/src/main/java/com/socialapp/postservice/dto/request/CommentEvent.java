package com.socialapp.postservice.dto.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommentEvent {
    String commentId;
    String postId;
    String storyId;
    String authorId;
    String groupId;
    String eventType;
    String receiverId;
}
