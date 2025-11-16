package com.socialapp.profileservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

import com.socialapp.profileservice.entity.UserProfile;

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
    String friendStatus;
    List<UserProfile> friendships;
    List<PostResponse.PostOne> posts;
}