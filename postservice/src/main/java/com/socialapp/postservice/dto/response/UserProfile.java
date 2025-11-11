package com.socialapp.postservice.dto.response;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
