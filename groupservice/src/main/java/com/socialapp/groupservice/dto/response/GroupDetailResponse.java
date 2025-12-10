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
    private String backgroundUrl;
    private String avatarUrl;
    private String privacy;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer memberCount;
    private GroupRole currentUserRole; // OWNER, ADMIN, MEMBER, NONE, PENDING
}

