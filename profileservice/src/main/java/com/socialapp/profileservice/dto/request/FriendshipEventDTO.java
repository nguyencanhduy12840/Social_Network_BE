package com.socialapp.profileservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FriendshipEventDTO {
    String senderId;
    String receiverId;
    String message;
    String type;
}
