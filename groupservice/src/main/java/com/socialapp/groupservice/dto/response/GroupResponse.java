package com.socialapp.groupservice.dto.response;

import com.socialapp.groupservice.util.constant.GroupPrivacy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupResponse {
    private String id;
    private String name;
    private String description;
    private String avatarUrl;
    private Integer memberCount;
    private GroupPrivacy privacy;
}
