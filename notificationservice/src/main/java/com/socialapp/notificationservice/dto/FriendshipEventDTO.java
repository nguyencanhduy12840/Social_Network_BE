package com.socialapp.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipEventDTO {
    private String type;
    private String senderId;
    private String receiverId;
    private String message;
}
