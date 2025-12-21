package com.socialapp.postservice.controller;

import com.socialapp.postservice.dto.request.SeenPostRequest;
import com.socialapp.postservice.dto.response.OneUserProfileResponse;
import com.socialapp.postservice.dto.response.PostResponse;
import com.socialapp.postservice.dto.response.PagedPostResponse;
import com.socialapp.postservice.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import com.socialapp.postservice.dto.request.LikePostRequest;
import com.socialapp.postservice.dto.response.CreatePostResponse;
import com.socialapp.postservice.entity.Post;
import com.socialapp.postservice.service.PostService;

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
        CreatePostResponse post = postService.createPost(userId, content, groupId, privacy, type, mediaFiles);
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
            @RequestParam String type) {
        PagedPostResponse pagedResponse = postService.getPostOnMainScreen(page, size, type);
        return ResponseEntity.ok().body(pagedResponse);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<PagedPostResponse> getGroupPosts(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedPostResponse pagedResponse = postService.getGroupPosts(groupId, page, size);
        return ResponseEntity.ok(pagedResponse);
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<PagedPostResponse> getUserPosts(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "POST") String type) {
        PagedPostResponse pagedResponse = postService.getUserPosts(userId, page, size, type);
        return ResponseEntity.ok(pagedResponse);
    }

    @PutMapping("/update-post")
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
    public ResponseEntity<List<OneUserProfileResponse.UserProfileOne>> getUsersWhoLikedPost(
            @PathVariable String postId) {
        List<OneUserProfileResponse.UserProfileOne> users = postService.getUserLikePost(postId);
        if (users == null) {
            throw new NotFoundException("Post not found");
        }
        return ResponseEntity.ok(users);
    }

    @PostMapping("/seen")
    public ResponseEntity<List<Post>> markPostsAsSeen(@RequestBody SeenPostRequest seenPostRequest) {
        List<Post> posts = postService.markPostsAsSeen(seenPostRequest);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/seen/{postId}")
    public ResponseEntity<List<OneUserProfileResponse.UserProfileOne>> getUsersWhoSeenPost(
            @PathVariable String postId) {
        List<OneUserProfileResponse.UserProfileOne> users = postService.getUsersWhoSeenPost(postId);
        if (users == null) {
            throw new NotFoundException("Post not found");
        }
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    public ResponseEntity<List<PostResponse>> searchPosts(
            @RequestParam String keyword) {
        List<PostResponse> posts = postService.searchPosts(keyword);
        return ResponseEntity.ok(posts);
    }
}
