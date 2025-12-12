package com.socialapp.profileservice.repository.httpclient;

import com.socialapp.profileservice.config.AuthenticationRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "group-service",
        path = "/group/internal",
        configuration = {AuthenticationRequestInterceptor.class})
public interface GroupClient {

    @GetMapping("/count")
    Integer getGroupCount(@RequestParam("userId") String userId);
}

