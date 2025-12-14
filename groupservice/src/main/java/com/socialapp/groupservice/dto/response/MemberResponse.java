package com.socialapp.groupservice.dto.response;

import com.socialapp.groupservice.util.constant.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MemberResponse {
    private UserResponse user;
    private GroupRole role;
}
