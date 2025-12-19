package com.socialapp.postservice.service;

import com.socialapp.postservice.dto.request.BaseEvent;
import com.socialapp.postservice.dto.request.CommentEvent;
import com.socialapp.postservice.dto.request.CreateCommentRequest;
import com.socialapp.postservice.dto.request.UpdateCommentRequest;
import com.socialapp.postservice.dto.response.CommentResponse;
import com.socialapp.postservice.dto.response.OneUserProfileResponse;
import com.socialapp.postservice.dto.response.PostResponse;
import com.socialapp.postservice.entity.Comment;
import com.socialapp.postservice.entity.Post;
import com.socialapp.postservice.mapper.CommentConverter;
import com.socialapp.postservice.repository.CommentRepository;
import com.socialapp.postservice.repository.PostRepository;
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
    private final PostRepository postRepository;

    public CommentService(CommentRepository commentRepository, KafkaTemplate<String, BaseEvent> kafkaTemplate,
                          CloudinaryService cloudinaryService, CommentConverter commentConverter,
                          ProfileClient profileClient, PostService postService, PostRepository postRepository) {
        this.profileClient = profileClient;
        this.commentConverter = commentConverter;
        this.cloudinaryService = cloudinaryService;
        this.kafkaTemplate = kafkaTemplate;
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.postRepository = postRepository;
    }

    public CommentResponse addComment(CreateCommentRequest comment, MultipartFile[] mediaFiles) {
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
            // Lấy parent comment để lấy authorId
            Comment parentComment = commentRepository.findById(savedComment.getParentCommentId())
                    .orElse(null);
            
            if(parentComment != null && !savedComment.getAuthorId().equals(parentComment.getAuthorId())) {
                CommentEvent commentEventToParent = CommentEvent.builder()
                        .commentId(savedComment.getId())
                        .postId(savedComment.getPostId())
                        .authorId(savedComment.getAuthorId())
                        .receiverId(parentComment.getAuthorId())
                        .groupId("")
                        .eventType("REPLY_COMMENT")
                        .build();

                BaseEvent baseEventParent = BaseEvent.builder()
                        .eventType("REPLY_COMMENT")
                        .sourceService("CommentService")
                        .payload(commentEventToParent).build();

                kafkaTemplate.send(NOTIFICATION_TOPIC, baseEventParent);
            }

            Post tempPost = postRepository.findById(savedComment.getPostId()).get();
            tempPost.setCommentsCount(tempPost.getCommentsCount() + 1);
            postRepository.save(tempPost);
            PostResponse post = postService.getPostById(savedComment.getPostId());

            if(!savedComment.getAuthorId().equals(post.getAuthorProfile().getId())){
                // Determine event type based on post type
                boolean isStory = "STORY".equals(post.getType());
                String commentEventType = isStory ? "COMMENT_ON_STORY" : "COMMENT_ON_POST";
                
                CommentEvent commentEventToPostOwner = CommentEvent.builder()
                        .commentId(savedComment.getId())
                        .postId(isStory ? null : savedComment.getPostId())
                        .storyId(isStory ? savedComment.getPostId() : null)
                        .authorId(savedComment.getAuthorId())
                        .receiverId(post.getAuthorProfile().getId())
                        .groupId("")
                        .eventType(commentEventType)
                        .build();



                BaseEvent baseEventPostOwner = BaseEvent.builder()
                        .eventType(commentEventType)
                        .sourceService("CommentService")
                        .payload(commentEventToPostOwner).build();

                kafkaTemplate.send(NOTIFICATION_TOPIC, baseEventPostOwner);
            }
        }
        else {
            PostResponse post = postService.getPostById(savedComment.getPostId());

            Post tempPost = postRepository.findById(savedComment.getPostId()).get();
            tempPost.setCommentsCount(tempPost.getCommentsCount() + 1);
            postRepository.save(tempPost);

            if(!savedComment.getAuthorId().equals(post.getAuthorProfile().getId())){
                // Determine event type based on post type
                boolean isStory = "STORY".equals(post.getType());
                String commentEventType = isStory ? "COMMENT_ON_STORY" : "COMMENT_ON_POST";
                
                CommentEvent commentEventToPostOwner = CommentEvent.builder()
                        .commentId(savedComment.getId())
                        .postId(isStory ? null : savedComment.getPostId())
                        .storyId(isStory ? savedComment.getPostId() : null)
                        .authorId(savedComment.getAuthorId())
                        .receiverId(post.getAuthorProfile().getId())
                        .groupId("")
                        .eventType(commentEventType)
                        .build();

                BaseEvent baseEvent = BaseEvent.builder()
                        .eventType(commentEventType)
                        .sourceService("CommentService")
                        .payload(commentEventToPostOwner).build();

                kafkaTemplate.send(NOTIFICATION_TOPIC, baseEvent);
            }
        }

        CommentResponse finalComment = commentConverter.toCommentResponse(savedComment);
        OneUserProfileResponse authorProfile = profileClient.getUserProfile(comment.getAuthorId());
        finalComment.setAuthorProfile(authorProfile.getData());

        return finalComment;
    }

    public List<CommentResponse> getCommentsByPostId(String postId) {
        List<CommentResponse> comments = new ArrayList<>();
        List<Comment> commentList = commentRepository.findByPostId(postId);
        for (Comment comment : commentList) {
            OneUserProfileResponse authorProfile = profileClient.getUserProfile(comment.getAuthorId());
            CommentResponse commentResponse = commentConverter.toCommentResponse(comment);
            commentResponse.setAuthorProfile(authorProfile.getData());
            comments.add(commentResponse);
        }
        return comments;
    }

    public void deleteComment(String commentId) {
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }
        Post post = postRepository.findById(comment.get().getPostId()).get();
        List<Comment> replies = commentRepository.findByParentCommentId(commentId);
        for (Comment reply : replies) {
            commentRepository.deleteById(reply.getId());
            post.setCommentsCount(post.getCommentsCount() - 1);
        }
        post.setCommentsCount(post.getCommentsCount() - 1);
        postRepository.save(post);
        commentRepository.deleteById(commentId);
    }

    public Comment getCommentById(String commentId) {
        Optional<Comment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }
        return comment.get();
    }

    public CommentResponse updateComment(UpdateCommentRequest comment, MultipartFile[] mediaFiles) {
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
            Comment oldComment = getCommentById(updatedComment.getId());
            updatedComment.setMedia(mediaUrls);
            updatedComment.setParentCommentId(oldComment.getParentCommentId());
            updatedComment.setPostId(oldComment.getPostId());
            updatedComment.setAuthorId(oldComment.getAuthorId());
            updatedComment.setCreatedAt(oldComment.getCreatedAt());
            updatedComment.setLikes(oldComment.getLikes());

            commentRepository.save(updatedComment);
            
            CommentResponse finalComment = commentConverter.toCommentResponse(updatedComment);
            OneUserProfileResponse authorProfile = profileClient.getUserProfile(updatedComment.getAuthorId());
            finalComment.setAuthorProfile(authorProfile.getData());

            return finalComment;
        }
        return null;
    }

    public Comment handleLikeAndDislike(String commentId, String userId) {
        Comment comment = getCommentById(commentId);
        boolean isLiked = false;
        
        if (comment.getLikes() == null) {
            comment.setLikes(new ArrayList<>());
        }
        
        if (comment.getLikes().contains(userId)) {
            comment.getLikes().remove(userId);
            isLiked = true;
        } else {
            comment.getLikes().add(userId);
        }
        Comment savedComment = commentRepository.save(comment);
        if(!isLiked) {
            if(!userId.equals(savedComment.getAuthorId())){
                CommentEvent commentEvent = CommentEvent.builder()
                        .commentId(savedComment.getId())
                        .postId(savedComment.getPostId())
                        .authorId(userId)
                        .groupId("")
                        .receiverId(savedComment.getAuthorId())
                        .eventType("LIKE_COMMENT")
                        .build();

                BaseEvent baseEvent = BaseEvent.builder()
                        .eventType("LIKE_COMMENT")
                        .sourceService("CommentService")
                        .payload(commentEvent).build();

                kafkaTemplate.send(NOTIFICATION_TOPIC, baseEvent);
            }
        }
        return savedComment;
    }
}
