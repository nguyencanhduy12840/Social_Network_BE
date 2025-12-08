package com.socialapp.groupservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveGroupResponse {
    private String groupId;
    private String groupName;
    private String userId;
    private Instant leftAt;
    private String message;
}

