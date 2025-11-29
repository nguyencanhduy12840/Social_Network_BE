package com.socialapp.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PostEventDTO {
    String postId;
    String authorId;
    String groupId;
    String eventType;
    String receiverId;
}
