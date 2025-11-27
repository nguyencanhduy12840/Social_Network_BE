package com.socialapp.profileservice.controller;

import com.socialapp.profileservice.dto.request.UpdateUserProfileRequest;
import com.socialapp.profileservice.dto.response.UserProfileResponse;
import com.socialapp.profileservice.entity.UserProfile;
import com.socialapp.profileservice.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserProfileController {
    private final UserProfileService userProfileService;
    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/users/{profileId}")
    ResponseEntity<UserProfileResponse> getProfile(@PathVariable String profileId) {
        return ResponseEntity.ok(userProfileService.getProfile(profileId));
    }

    @GetMapping("/users")
    ResponseEntity<List<UserProfileResponse>> getAllProfiles() {
        return ResponseEntity.ok(userProfileService.getAllProfiles());
    }

    @PutMapping("/users")
    ResponseEntity<UserProfile> updateProfile(UpdateUserProfileRequest profile) {
        return ResponseEntity.ok(userProfileService.updateProfile(profile));
    }
}
