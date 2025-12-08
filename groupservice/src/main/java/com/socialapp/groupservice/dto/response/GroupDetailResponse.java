package com.socialapp.groupservice.dto.response;

import com.socialapp.groupservice.util.constant.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDetailResponse {
    private String id;
    private String name;
    private String ownerId;
    private String description;
    private String backgroundImageUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer memberCount;
    private GroupRole currentUserRole; // Vai trò của người dùng hiện tại trong nhóm
    private Boolean isMember; // Người dùng hiện tại có phải là thành viên không
    private Boolean isOwner; // Người dùng hiện tại có phải là chủ nhóm không
}

