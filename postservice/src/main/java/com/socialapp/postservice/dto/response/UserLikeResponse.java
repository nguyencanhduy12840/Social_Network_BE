package com.socialapp.postservice.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserLikeResponse {
    List<UserProfileOne> data;
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class UserProfileOne {
        String id;

        String userId;

        String email;
        String firstName;
        String lastName;

        String username;
        String avatarUrl;
        String gender;
        LocalDate dob;
    }
}
