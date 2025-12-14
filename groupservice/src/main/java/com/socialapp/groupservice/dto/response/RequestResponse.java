package com.socialapp.groupservice.dto.response;

import com.socialapp.groupservice.util.constant.JoinRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RequestResponse {
    private String id;
    private GroupResponse group;
    private UserResponse user;
    private JoinRequestStatus status;
}
