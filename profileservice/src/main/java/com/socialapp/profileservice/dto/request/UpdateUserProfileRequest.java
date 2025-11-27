package com.socialapp.profileservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserProfileRequest {
    String userId;
    String firstName;
    String lastName;
    String username;
    String bio;
    String gender;
    LocalDate dob;
}
