package com.socialapp.postservice.controller;

import com.socialapp.postservice.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class InternalPostController {
    private final PostService postService;

    public InternalPostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> countPostsByUserId(@RequestParam String userId) {
        return ResponseEntity.ok(postService.countPostsByUserId(userId));
    }
}
