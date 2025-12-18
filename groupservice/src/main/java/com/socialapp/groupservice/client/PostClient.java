package com.socialapp.groupservice.client;

import com.socialapp.groupservice.config.AuthenticationRequestInterceptor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "post-service",
            path = "/post/internal",
            configuration = {AuthenticationRequestInterceptor.class})
public interface PostClient {
    
    @DeleteMapping("/group/{groupId}")
    void deletePostsByGroupId(@PathVariable String groupId);
    
    @DeleteMapping("/group/{groupId}/author/{authorId}")
    void deletePostsByGroupIdAndAuthorId(
        @PathVariable String groupId, 
        @PathVariable String authorId
    );
}
