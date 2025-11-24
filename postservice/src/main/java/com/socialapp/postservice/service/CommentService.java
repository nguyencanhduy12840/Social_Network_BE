package com.socialapp.postservice.service;

import com.socialapp.postservice.dto.request.BaseEvent;
import com.socialapp.postservice.dto.request.CreateCommentRequest;
import com.socialapp.postservice.dto.request.UpdateCommentRequest;
import com.socialapp.postservice.entity.Comment;
import com.socialapp.postservice.mapper.CommentConverter;
import com.socialapp.postservice.repository.CommentRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final KafkaTemplate<String, BaseEvent> kafkaTemplate;
    private final CloudinaryService cloudinaryService;
    private final CommentConverter commentConverter;

    private final String NOTIFICATION_TOPIC = "notification-events";

    public CommentService(CommentRepository commentRepository, KafkaTemplate<String, BaseEvent> kafkaTemplate,
                          CloudinaryService cloudinaryService, CommentConverter commentConverter) {
        this.commentConverter = commentConverter;
        this.cloudinaryService = cloudinaryService;
        this.kafkaTemplate = kafkaTemplate;
        this.commentRepository = commentRepository;
    }

    public Comment addComment(CreateCommentRequest comment, MultipartFile[] mediaFiles) {
        List<String> mediaUrls = new ArrayList<>();

        if (mediaFiles != null && mediaFiles.length > 0) {
            for (MultipartFile file : mediaFiles) {
                String fileType = file.getContentType();
                String url;

                if (fileType != null && fileType.startsWith("video")) {
                    url = cloudinaryService.uploadVideo(file);
                } else {
                    url = cloudinaryService.uploadImage(file);
                }

                mediaUrls.add(url);
            }
        }
        Comment newComment = commentConverter.toComment(comment);
        newComment.setMedia(mediaUrls);
        return commentRepository.save(newComment);
    }

    public List<Comment> getCommentsByPostId(String postId) {
        return commentRepository.findByPostId(postId);
    }

    public void deleteComment(String commentId) {
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }
        commentRepository.deleteById(commentId);
    }

    public Comment getCommentById(String commentId) {
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }
        return comment.get();
    }

    public Comment updateComment(UpdateCommentRequest comment, MultipartFile[] mediaFiles) {
        Comment updatedComment = commentConverter.toCommentFromUpdate(comment);
        List<String> mediaUrls = new ArrayList<>();

        if (mediaFiles != null && mediaFiles.length > 0) {
            for (MultipartFile file : mediaFiles) {
                String fileType = file.getContentType();
                String url;

                if (fileType != null && fileType.startsWith("video")) {
                    url = cloudinaryService.uploadVideo(file);
                } else {
                    url = cloudinaryService.uploadImage(file);
                }

                mediaUrls.add(url);
            }
        }
        if(getCommentById(updatedComment.getId()) != null) {
            updatedComment.setMedia(mediaUrls);
            return commentRepository.save(updatedComment);
        }
        return null;
    }

    public Comment handleLikeAndDislike(String commentId, String userId) {
        Comment comment = getCommentById(commentId);
        if (comment.getLikes().contains(userId)) {
            comment.getLikes().remove(userId);
        } else {
            comment.getLikes().add(userId);
        }
        return commentRepository.save(comment);
    }
}
