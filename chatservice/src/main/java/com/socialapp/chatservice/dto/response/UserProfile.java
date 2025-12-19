package com.socialapp.chatservice.dto.response;

import java.util.List;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    List<UserResponse> data;
}