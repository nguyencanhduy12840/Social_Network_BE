package com.socialapp.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentEventDTO {
    String commentId;
    String postId;
    String authorId;
    String groupId;
    String eventType;
    String receiverId;
}
