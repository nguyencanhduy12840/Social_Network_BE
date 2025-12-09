package com.socialapp.postservice.repository.httpclient;

import com.socialapp.postservice.config.AuthenticationRequestInterceptor;
import com.socialapp.postservice.dto.response.ApiResponse;
import com.socialapp.postservice.dto.response.GroupMemberResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "group-service",
        configuration = {AuthenticationRequestInterceptor.class})
public interface GroupClient {

    @GetMapping(value = "/group/internal/{groupId}/members")
    ApiResponse<List<GroupMemberResponse>> getGroupMembers(@PathVariable String groupId);
}

