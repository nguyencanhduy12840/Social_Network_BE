package com.socialapp.groupservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HandleJoinRequestRequest {
    private String groupId;
    private String requestId;
    private Boolean approved;
}
