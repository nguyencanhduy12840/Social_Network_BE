package com.socialapp.groupservice.dto.request;

import com.socialapp.groupservice.util.constant.GroupRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMemberRoleRequest {
    private GroupRole role;
}

