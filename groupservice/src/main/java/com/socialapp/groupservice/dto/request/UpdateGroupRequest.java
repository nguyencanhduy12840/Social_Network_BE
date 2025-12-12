package com.socialapp.groupservice.dto.request;

import com.socialapp.groupservice.util.constant.GroupPrivacy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateGroupRequest {
    private String groupId;

    private String name;
    
    private GroupPrivacy privacy;

    private String description;
}
