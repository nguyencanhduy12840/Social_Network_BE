package com.socialapp.notificationservice.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    private String id;
    private String receiverId;
    private String senderId;
    private String type;
    private boolean isRead;
    private Instant createdAt;
    private ExtraData extraData;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ExtraData {
        private String postId;
        private String storyId;
        private String commentId;
        private String groupId;
    }
}
