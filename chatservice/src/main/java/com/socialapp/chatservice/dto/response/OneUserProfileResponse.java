package com.socialapp.chatservice.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OneUserProfileResponse {
    UserProfileOne data;
    
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
        String friendStatus;
        LocalDate dob;
    }
}
