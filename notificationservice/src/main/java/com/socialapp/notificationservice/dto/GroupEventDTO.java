package com.socialapp.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupEventDTO {
    String groupId;
    String groupName;
    String senderId;
    String receiverId;
    String type;
    String newRole;
}
