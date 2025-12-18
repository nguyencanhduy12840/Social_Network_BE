package com.socialapp.postservice.controller;

import com.socialapp.postservice.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/group/{groupId}")
    public ResponseEntity<Void> deletePostsByGroupId(@PathVariable String groupId) {
        postService.deletePostsByGroupId(groupId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/group/{groupId}/author/{authorId}")
    public ResponseEntity<Void> deletePostsByGroupIdAndAuthorId(
            @PathVariable String groupId,
            @PathVariable String authorId) {
        postService.deletePostsByGroupIdAndAuthorId(groupId, authorId);
        return ResponseEntity.noContent().build();
    }
}
