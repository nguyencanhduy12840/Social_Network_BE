package com.socialapp.postservice.controller;

import com.socialapp.postservice.dto.request.SeenStoryRequest;
import com.socialapp.postservice.dto.response.OneUserProfileResponse;
import com.socialapp.postservice.dto.response.PostResponse;
import com.socialapp.postservice.dto.response.PagedPostResponse;
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
            @RequestParam("type") String type,
            @RequestPart(value = "media", required = false) MultipartFile[] mediaFiles) {
                CreatePostResponse post = postService.createPost(userId, content, groupId, privacy,type,  mediaFiles);
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
     public ResponseEntity<PagedPostResponse> getPostDisplay(
             @RequestParam(defaultValue = "0") int page,
             @RequestParam(defaultValue = "10") int size,
             @RequestParam String type){
        PagedPostResponse pagedResponse = postService.getPostOnMainScreen(page, size, type);
        return ResponseEntity.ok().body(pagedResponse);
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
     public ResponseEntity<PostResponse> getPostById(@PathVariable String postId) {
        PostResponse post = postService.getPostById(postId);
        if (post == null) {
            throw new NotFoundException("Post not found");
        }
        return ResponseEntity.ok(post);
     }

     @GetMapping("/userlikes/{postId}")
    public ResponseEntity<List<OneUserProfileResponse.UserProfileOne>> getUsersWhoLikedPost(@PathVariable String postId) {
        List<OneUserProfileResponse.UserProfileOne> users = postService.getUserLikePost(postId);
        if (users == null) {
            throw new NotFoundException("Post not found");
        }
        return ResponseEntity.ok(users);
    }

    @PostMapping("/story/seen")
    public ResponseEntity<Post> seenStory(@RequestBody SeenStoryRequest seenStoryRequest) {
        Post post = postService.seenStory(seenStoryRequest);
        if(post == null) {
            throw new NotFoundException("Post not found");
        }
        return ResponseEntity.ok(post);
    }
}
