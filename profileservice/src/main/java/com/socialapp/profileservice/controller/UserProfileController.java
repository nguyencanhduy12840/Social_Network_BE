package com.socialapp.profileservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialapp.profileservice.dto.request.UpdateUserProfileRequest;
import com.socialapp.profileservice.dto.response.UserProfileResponse;
import com.socialapp.profileservice.entity.UserProfile;
import com.socialapp.profileservice.service.UserProfileService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class UserProfileController {
    private final UserProfileService userProfileService;
    private final ObjectMapper mapper;

    public UserProfileController(UserProfileService userProfileService, ObjectMapper mapper) {
        this.userProfileService = userProfileService;
        this.mapper = mapper;
    }

    @GetMapping("/users/{profileId}")
    ResponseEntity<UserProfileResponse> getProfile(@PathVariable String profileId) {
        return ResponseEntity.ok(userProfileService.getProfile(profileId));
    }

    @GetMapping("/users")
    ResponseEntity<List<UserProfileResponse>> getAllProfiles() {
        return ResponseEntity.ok(userProfileService.getAllProfiles());
    }

    @PutMapping(value = "/users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<UserProfile> updateUserProfile(
            @RequestPart("profile") String profile,
            @RequestPart(value = "media", required = false) MultipartFile mediaFile) throws JsonProcessingException {

        UpdateUserProfileRequest request = mapper.readValue(profile, UpdateUserProfileRequest.class);

        return ResponseEntity.ok(userProfileService.updateProfile(request, mediaFile));
    }

    @GetMapping("/users/search")
    ResponseEntity<List<UserProfileResponse>> searchUsers(
            @RequestParam String keyword) {
        return ResponseEntity.ok(userProfileService.searchUsersByUsername(keyword));
    }
}
