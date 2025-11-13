package com.socialapp.postservice.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialapp.postservice.entity.Post;
import com.socialapp.postservice.service.PostService;

@RestController
@RequestMapping("/internal")
public class InternalPostController {
    private final PostService postService;
    public InternalPostController(PostService postService) {
        this.postService = postService;
    }
    @GetMapping("/{userId}")
    public List<Post> getUserPosts(@PathVariable String userId) {
        return postService.getPostsByUserId(userId);
    }
}
