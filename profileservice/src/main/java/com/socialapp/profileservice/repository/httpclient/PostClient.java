package com.socialapp.profileservice.repository.httpclient;

import com.socialapp.profileservice.config.AuthenticationRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "post-service",
        path = "/post/internal",
        configuration = {AuthenticationRequestInterceptor.class})
public interface PostClient{

    @GetMapping("/count")
    Integer getPostCount(@RequestParam String userId);
}
