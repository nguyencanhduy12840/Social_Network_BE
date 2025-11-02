package com.socialapp.identityservice.repository.httpclient;

import com.socialapp.identityservice.config.AuthenticationRequestInterceptor;
import com.socialapp.identityservice.dto.request.ProfileCreationRequest;
import com.socialapp.identityservice.dto.response.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "profile-service",
        configuration = {AuthenticationRequestInterceptor.class} )
public interface ProfileClient {
    @PostMapping(value = "/profile/internal/users", produces = MediaType.APPLICATION_JSON_VALUE)
    UserProfileResponse createProfile(@RequestBody ProfileCreationRequest request);

}
