package com.socialapp.postservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialapp.postservice.dto.request.CreateCommentRequest;
import com.socialapp.postservice.dto.request.LikeCommentRequest;
import com.socialapp.postservice.dto.request.UpdateCommentRequest;
import com.socialapp.postservice.dto.response.CommentResponse;
import com.socialapp.postservice.entity.Comment;
import com.socialapp.postservice.exception.NotFoundException;
import com.socialapp.postservice.service.CommentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentResponse> createComment(
            @RequestPart("comment") String requestJson,
            @RequestPart(value = "media", required = false) MultipartFile[] mediaFiles) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        CreateCommentRequest request = mapper.readValue(requestJson, CreateCommentRequest.class);

        return ResponseEntity.ok(commentService.addComment(request, mediaFiles));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable String postId) {
        return ResponseEntity.ok(commentService.getCommentsByPostId(postId));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable String commentId) {
        if(commentService.getCommentById(commentId.toString()) == null) {
            throw new NotFoundException("Comment not found");
        }
        commentService.deleteComment(commentId);
        return ResponseEntity.ok("Comment deleted successfully");
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentResponse> updateComment(
            @RequestPart("comment") String requestJson,
            @RequestPart(value = "media", required = false) MultipartFile[] mediaFiles) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        UpdateCommentRequest updateCommentRequest = mapper.readValue(requestJson, UpdateCommentRequest.class);

        return ResponseEntity.ok(commentService.updateComment(updateCommentRequest, mediaFiles));
    }

    @PostMapping("/like")
    public ResponseEntity<Comment> likeComment(@RequestBody LikeCommentRequest likeCommentRequest){
        return ResponseEntity.ok(commentService.handleLikeAndDislike(likeCommentRequest.getCommentId(), likeCommentRequest.getUserId()));
    }
}
