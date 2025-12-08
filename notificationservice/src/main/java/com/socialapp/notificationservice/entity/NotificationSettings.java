package com.socialapp.notificationservice.entity;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "notification_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettings {
    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private boolean enabled;

    private Map<String, Boolean> settings; // Key: NotificationType, Value: Enabled

    private Instant createdAt;
    private Instant updatedAt;
}
