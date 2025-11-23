package com.socialapp.postservice.service;

import com.socialapp.postservice.dto.request.BaseEvent;
import com.socialapp.postservice.dto.request.CreateCommentRequest;
import com.socialapp.postservice.entity.Comment;
import com.socialapp.postservice.mapper.CommentConverter;
import com.socialapp.postservice.repository.CommentRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

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
}
