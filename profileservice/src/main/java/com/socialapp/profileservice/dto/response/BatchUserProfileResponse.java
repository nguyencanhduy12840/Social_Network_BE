package com.socialapp.profileservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchUserProfileResponse {
    Map<String, UserProfileResponse> profiles;
}
