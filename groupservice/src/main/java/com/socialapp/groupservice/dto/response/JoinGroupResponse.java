package com.socialapp.groupservice.dto.response;

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
public class JoinGroupResponse {
    private String id;
    private String groupId;
    private String groupName;
    private String userId;
    private JoinRequestStatus status;
    private Instant requestedAt;
}

