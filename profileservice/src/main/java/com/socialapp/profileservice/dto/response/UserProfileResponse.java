package com.socialapp.profileservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {
    String id;
    String email;
    String firstName;
    String lastName;
    String username;
    String avatarUrl;
    String bio;
    String gender;
    LocalDate dob;
    Integer postCount;
    Integer groupCount;
    Integer friendCount;
    String friendStatus;
}