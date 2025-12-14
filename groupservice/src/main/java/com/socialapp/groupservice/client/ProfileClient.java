package com.socialapp.groupservice.client;

import com.socialapp.groupservice.config.AuthenticationRequestInterceptor;
import com.socialapp.groupservice.dto.response.OneUserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service",
             configuration = {AuthenticationRequestInterceptor.class})
public interface ProfileClient {
    
    @GetMapping("/profile/internal/users/{userId}")
    OneUserProfileResponse getUserProfile(@PathVariable String userId);
}
