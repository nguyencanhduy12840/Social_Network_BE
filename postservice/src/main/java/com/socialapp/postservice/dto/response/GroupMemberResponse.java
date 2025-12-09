package com.socialapp.postservice.dto.response;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {
    private String id;
    private String userId;
    private String groupId;
    private String role;
    private Instant joinedAt;
}

