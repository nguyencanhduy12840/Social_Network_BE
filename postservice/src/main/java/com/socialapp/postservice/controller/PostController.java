package com.socialapp.postservice.controller;

import com.socialapp.postservice.dto.request.UpdatePostRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;


import com.socialapp.postservice.dto.request.LikePostRequest;
import com.socialapp.postservice.dto.response.CreatePostResponse;
import com.socialapp.postservice.entity.Post;
import com.socialapp.postservice.service.PostService;

import jakarta.ws.rs.NotFoundException;

import java.util.List;

@RestController
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/create-post")
    public ResponseEntity<CreatePostResponse> createPost(
            @RequestParam("userId") String userId,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String groupId,
            @RequestParam("privacy") String privacy,
            @RequestPart(value = "media", required = false) MultipartFile[] mediaFiles) {
                CreatePostResponse post = postService.createPost(userId, content, groupId, privacy, mediaFiles);
                return ResponseEntity.ok(post);
            }
    
    @PostMapping("/like-post")
    public ResponseEntity<Post> likePost(@RequestBody LikePostRequest likePostRequest) {
        Post updatedPost = postService.handleLikePost(likePostRequest);
        if (updatedPost == null) {
            throw new NotFoundException("Post not found");
        }
        return ResponseEntity.ok(updatedPost);
    }

     @GetMapping("/get-post")
     public ResponseEntity<List<Post>> getPostDisplay(){
        List<Post> postList = postService.getPostOnMainScreen();
        return ResponseEntity.ok().body(postList);
     }

     @PostMapping("/update-post")
     public ResponseEntity<Post> updatePost(@RequestParam("postId") String postId,
                                            @RequestParam(required = false) String content,
                                            @RequestParam("privacy") String privacy,
                                            @RequestPart(value = "media", required = false) MultipartFile[] mediaFiles) {
        Post updatedPost = postService.updatePost(postId, content, privacy, mediaFiles);
        if (updatedPost == null) {
            throw new NotFoundException("Post not found");
        }
        return ResponseEntity.ok(updatedPost);
     }

     @DeleteMapping("/delete-post/{postId}")
     public ResponseEntity<String> deletePost(@PathVariable String postId) {
        boolean isDeleted = postService.deletePost(postId);
        if (!isDeleted) {
            throw new NotFoundException("Post not found");
        }
        return ResponseEntity.ok("Post deleted successfully");
     }

     @PostMapping("/unlike-post")
     public ResponseEntity<Post> unlikePost(@RequestBody LikePostRequest unlikePostRequest) {
        Post updatedPost = postService.unlikePost(unlikePostRequest);
        if (updatedPost == null) {
            throw new NotFoundException("Post not found");
        }
        return ResponseEntity.ok(updatedPost);
     }

     @GetMapping("/{postId}")
     public ResponseEntity<Post> getPostById(@PathVariable String postId) {
        Post post = postService.getPostById(postId);
        if (post == null) {
            throw new NotFoundException("Post not found");
        }
        return ResponseEntity.ok(post);
     }
}
