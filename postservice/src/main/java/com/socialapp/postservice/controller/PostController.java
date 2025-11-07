package com.socialapp.postservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.socialapp.postservice.dto.request.CreatePostRequest;
import com.socialapp.postservice.dto.response.CreatePostResponse;
import com.socialapp.postservice.service.PostService;

@RestController
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/create-post")
    public ResponseEntity<CreatePostResponse> createPost(
            @RequestParam("userId") String userId, 
            @RequestParam String content, 
            @RequestParam(required = false) String groupId, 
            @RequestParam("privacy") String privacy,
            @RequestPart("media") MultipartFile[] mediaFiles) {
                CreatePostResponse post = postService.createPost(userId, content, groupId, privacy, mediaFiles);
                return ResponseEntity.ok(post);
            }
}
