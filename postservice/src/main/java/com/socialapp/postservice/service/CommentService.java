package com.socialapp.postservice.service;

import com.socialapp.postservice.dto.request.BaseEvent;
import com.socialapp.postservice.dto.request.CommentEvent;
import com.socialapp.postservice.dto.request.CreateCommentRequest;
import com.socialapp.postservice.dto.request.UpdateCommentRequest;
import com.socialapp.postservice.dto.response.OneUserProfileResponse;
import com.socialapp.postservice.dto.response.PostResponse;
import com.socialapp.postservice.entity.Comment;
import com.socialapp.postservice.entity.Post;
import com.socialapp.postservice.mapper.CommentConverter;
import com.socialapp.postservice.repository.CommentRepository;
import com.socialapp.postservice.repository.httpclient.ProfileClient;
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

    private final ProfileClient profileClient;

    private final String NOTIFICATION_TOPIC = "notification-events";
    private final PostService postService;

    public CommentService(CommentRepository commentRepository, KafkaTemplate<String, BaseEvent> kafkaTemplate,
                          CloudinaryService cloudinaryService, CommentConverter commentConverter, ProfileClient profileClient, PostService postService) {
        this.profileClient = profileClient;
        this.commentConverter = commentConverter;
        this.cloudinaryService = cloudinaryService;
        this.kafkaTemplate = kafkaTemplate;
        this.commentRepository = commentRepository;
        this.postService = postService;
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
        Comment savedComment = commentRepository.save(newComment);
        if(savedComment.getParentCommentId() != null) {
            String peopleReply = profileClient.getUserProfile(
                    savedComment.getAuthorId()
            ).getData().getUsername();
            CommentEvent commentEventToParent = CommentEvent.builder()
                    .commentId(savedComment.getId())
                    .postId(savedComment.getPostId())
                    .authorId(savedComment.getAuthorId())
                    .receiverId(savedComment.getParentCommentId())
                    .eventType("REPLY_COMMENT")
                    .content(peopleReply + "replied to your comment")
                    .build();

            PostResponse post = postService.getPostById(savedComment.getPostId());

            CommentEvent commentEventToPostOwner = CommentEvent.builder()
                    .commentId(savedComment.getId())
                    .postId(savedComment.getPostId())
                    .authorId(savedComment.getAuthorId())
                    .receiverId(post.getAuthorProfile().getId())
                    .eventType("COMMENT_ON_POST")
                    .content(peopleReply + "commented on your post")
                    .build();

            BaseEvent baseEventParent = BaseEvent.builder()
                    .eventType("REPLY_COMMENT")
                            .sourceService("CommentService")
                    .payload(commentEventToParent).build();

            kafkaTemplate.send(NOTIFICATION_TOPIC, baseEventParent);

            BaseEvent baseEventPostOwner = BaseEvent.builder()
                    .eventType("COMMENT_ON_POST")
                            .sourceService("CommentService")
                    .payload(commentEventToPostOwner).build();

            kafkaTemplate.send(NOTIFICATION_TOPIC, baseEventPostOwner);
        }
        else{
            String peopleComment = profileClient.getUserProfile(
                    savedComment.getAuthorId()
            ).getData().getUsername();
            PostResponse post = postService.getPostById(savedComment.getPostId());

            CommentEvent commentEventToPostOwner = CommentEvent.builder()
                    .commentId(savedComment.getId())
                    .postId(savedComment.getPostId())
                    .authorId(savedComment.getAuthorId())
                    .receiverId(post.getAuthorProfile().getId())
                    .eventType("COMMENT_ON_POST")
                    .content(peopleComment + " commented on your post")
                    .build();

            BaseEvent baseEvent = BaseEvent.builder()
                    .eventType("COMMENT_ON_POST")
                            .sourceService("CommentService")
                    .payload(commentEventToPostOwner).build();

            kafkaTemplate.send(NOTIFICATION_TOPIC, baseEvent);

        }

        return savedComment;
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
