package com.socialapp.groupservice.dto.response;

import com.socialapp.groupservice.util.constant.GroupPrivacy;
import com.socialapp.groupservice.util.constant.GroupRole;
import com.socialapp.groupservice.util.constant.JoinRequestStatus;
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
    private String avatarUrl;
    private String backgroundUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer memberCount;
    private GroupPrivacy privacy;
    private GroupRole role;
    private JoinRequestStatus joinStatus;
}

