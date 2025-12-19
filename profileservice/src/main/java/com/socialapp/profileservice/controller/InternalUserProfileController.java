package com.socialapp.profileservice.controller;

import com.socialapp.profileservice.dto.request.ProfileCreationRequest;
import com.socialapp.profileservice.dto.response.UserProfileResponse;
import com.socialapp.profileservice.service.UserProfileService;
import org.springframework.web.bind.annotation.*;

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
}
