package com.socialapp.groupservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupEventDTO {
    private String groupId;
    private String groupName;
    private String senderId;
    private String receiverId;
    private String type;
    private String newRole;
}
