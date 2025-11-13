package com.socialapp.profileservice.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.socialapp.profileservice.config.AuthenticationRequestInterceptor;
import com.socialapp.profileservice.dto.response.PostResponse;

@FeignClient(name = "post-service",
        configuration = {AuthenticationRequestInterceptor.class} )
public interface PostClient {
    @GetMapping(value = "/post/internal/{userId}")
    PostResponse getPosts(@PathVariable String userId);
}
