package com.socialapp.chatservice.client;

import com.socialapp.chatservice.config.AuthenticationRequestInterceptor;
import com.socialapp.chatservice.dto.response.OneUserProfileResponse;
import com.socialapp.chatservice.dto.response.UserProfile;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service", configuration = { AuthenticationRequestInterceptor.class })
public interface ProfileClient {

    @GetMapping("/profile/internal/users/{userId}")
    OneUserProfileResponse getUserProfile(@PathVariable String userId);

    @GetMapping("/profile/internal/friendships/{userId}")
    UserProfile getFriends(@PathVariable String userId);
}