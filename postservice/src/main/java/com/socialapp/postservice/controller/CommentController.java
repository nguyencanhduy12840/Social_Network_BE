package com.socialapp.postservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialapp.postservice.dto.request.CreateCommentRequest;
import com.socialapp.postservice.entity.Comment;
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
    public ResponseEntity<Comment> createComment(
            @RequestPart("comment") String requestJson,
            @RequestPart(value = "media", required = false) MultipartFile[] mediaFiles) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        CreateCommentRequest request = mapper.readValue(requestJson, CreateCommentRequest.class);

        return ResponseEntity.ok(commentService.addComment(request, mediaFiles));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<List<Comment>> getComments(@PathVariable String postId) {
        return ResponseEntity.ok(commentService.getCommentsByPostId(postId.toString()));
    }
}
