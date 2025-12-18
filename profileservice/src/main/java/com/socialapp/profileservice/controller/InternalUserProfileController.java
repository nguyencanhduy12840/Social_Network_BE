package com.socialapp.profileservice.controller;

import com.socialapp.profileservice.dto.request.ProfileCreationRequest;
import com.socialapp.profileservice.dto.response.BatchUserProfileResponse;
import com.socialapp.profileservice.dto.response.UserProfileResponse;
import com.socialapp.profileservice.service.UserProfileService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class InternalUserProfileController {
    private final UserProfileService userProfileService;
    public InternalUserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }
    @PostMapping("/internal/users")
    UserProfileResponse createProfile(@RequestBody ProfileCreationRequest request){
        return userProfileService.createProfile(request);
    }

    @GetMapping("/internal/users/{userId}")
    UserProfileResponse getUserProfile(@PathVariable String userId) {
        return userProfileService.getProfileById(userId);
    }

    @PostMapping("/internal/users/batch")
    BatchUserProfileResponse getUserProfiles(@RequestBody List<String> userIds) {
        Map<String, UserProfileResponse> profiles = userProfileService.getUserProfilesByIds(userIds);
        return BatchUserProfileResponse.builder()
                .profiles(profiles)
                .build();
    }
}
