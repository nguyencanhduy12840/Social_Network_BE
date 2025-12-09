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
public class UpdateGroupResponse {
    private String id;
    private String name;
    private String ownerId;
    private String description;
    private String backgroundImageUrl;
    private Instant updatedAt;
    private String message;
}

