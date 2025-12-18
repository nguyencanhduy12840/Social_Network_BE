package com.socialapp.postservice.dto.response;

import java.util.List;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    List<UserProfileOne> data;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class UserProfileOne {
        String id;
        String username;
        String avatarUrl;
    }
}

