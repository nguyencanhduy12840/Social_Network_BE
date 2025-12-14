package com.socialapp.chatservice.client;

import com.socialapp.chatservice.dto.response.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service")
public interface ProfileClient {

    @GetMapping("/profile/internal/users/{userId}")
    UserProfileResponse getUserProfile(@PathVariable("userId") String userId);
}

