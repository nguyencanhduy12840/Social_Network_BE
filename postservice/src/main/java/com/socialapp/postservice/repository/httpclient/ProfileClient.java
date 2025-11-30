package com.socialapp.postservice.repository.httpclient;

import java.util.List;

import com.socialapp.postservice.dto.response.OneUserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

;

import com.socialapp.postservice.config.AuthenticationRequestInterceptor;
import com.socialapp.postservice.dto.response.ApiResponse;
import com.socialapp.postservice.dto.response.UserProfile;


@FeignClient(name = "profile-service",
        configuration = {AuthenticationRequestInterceptor.class} )
public interface ProfileClient {
    @GetMapping(value = "/profile/internal/friendships/{userId}")
    UserProfile getFriends(@PathVariable String userId);

    @GetMapping(value = "/profile/internal/friendships/isFriend/{userId}/{friendId}")
    ApiResponse<Boolean> isFriend(@PathVariable String userId, @PathVariable String friendId);

    @GetMapping(value = "/profile/internal/users/{userId}")
    OneUserProfileResponse getUserProfile(@PathVariable String userId);
}
