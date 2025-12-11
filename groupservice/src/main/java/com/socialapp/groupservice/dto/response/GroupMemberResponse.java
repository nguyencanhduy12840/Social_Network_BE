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
public class GroupMemberResponse {
    private String userId;
    private String groupId;
    private GroupRole role;
    private Instant joinedAt;
}

