package com.socialapp.chatservice.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileResponse {
    private String userId;
    private String fullName;
    private String avatarUrl;
    private String email;
}
