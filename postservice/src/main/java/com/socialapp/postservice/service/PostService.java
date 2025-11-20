package com.socialapp.postservice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import com.socialapp.postservice.dto.request.UpdatePostRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nimbusds.jose.proc.SecurityContext;
import com.socialapp.postservice.dto.request.BaseEvent;
import com.socialapp.postservice.dto.request.LikePostRequest;
import com.socialapp.postservice.dto.request.PostEvent;
import com.socialapp.postservice.dto.response.CreatePostResponse;
import com.socialapp.postservice.dto.response.UserProfile;
import com.socialapp.postservice.entity.Post;
import com.socialapp.postservice.mapper.PostConverter;
import com.socialapp.postservice.repository.PostRepository;
import com.socialapp.postservice.repository.httpclient.ProfileClient;
import com.socialapp.postservice.util.SecurityUtil;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostConverter postConverter;
    private final CloudinaryService cloudinaryService;

    private final KafkaTemplate<String, BaseEvent> kafkaTemplate;
    private final ProfileClient profileClient;

    private final String NOTIFICATION_TOPIC = "notification-events";

    public PostService(PostRepository postRepository, PostConverter postConverter, CloudinaryService cloudinaryService, KafkaTemplate<String, BaseEvent> kafkaTemplate, ProfileClient profileClient) {
        this.postRepository = postRepository;
        this.postConverter = postConverter;
        this.cloudinaryService = cloudinaryService;
        this.kafkaTemplate = kafkaTemplate;
        this.profileClient = profileClient;
    }

    public CreatePostResponse createPost(String userId, String content, String groupId, String privacy, MultipartFile[] mediaFiles) {
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

        String type = (groupId == null || groupId.isEmpty()) ? "personal" : "group";

        Post post = Post.builder()
                .authorId(userId)
                .groupId(groupId)
                .type(type)
                .content(content)
                .privacy(privacy)
                .media(mediaUrls)
                .createdAt(Instant.now())
                .likes(new ArrayList<>())
                .commentsCount(0)
                .build();
        

        Post savedPost = postRepository.save(post);

        if(savedPost.getPrivacy().equals("PUBLIC") || savedPost.getPrivacy().equals("FRIENDS")){
            //send notifications to friends
            UserProfile friends = profileClient.getFriends(userId);
            friends.getData().parallelStream().forEach(friend-> {
                PostEvent event = PostEvent.builder()
                                .postId(savedPost.getId())
                                .authorId(userId)
                                .content(content)
                                .eventType("NEW_POST")
                                .receiverId(friend.getUserId())
                                .build();

                            BaseEvent baseEvent = BaseEvent.builder()
                                .eventType("NEW_POST")
                                .sourceService("PostService")
                                .payload(event)
                                .build();

                        kafkaTemplate.send(NOTIFICATION_TOPIC, baseEvent);
            });
        }
        return postConverter.convertToCreatePostResponse(savedPost);
    }

    public Post handleLikePost(LikePostRequest likePostRequest) {
        Optional<Post> post = postRepository.findById(likePostRequest.getPostId());
        if(post.isPresent()){
            Post existingPost = post.get();
            List<String> likes = existingPost.getLikes();
            if (likes.contains(likePostRequest.getUserId())) {
                likes.remove(likePostRequest.getUserId());
            } else {
                likes.add(likePostRequest.getUserId());
            }
            existingPost.setLikes(likes);
            return postRepository.save(existingPost);
        }
        return null;
    }

    public List<Post> getPostsByUserId(String userId) {
        Optional<String> requestId = SecurityUtil.getCurrentUserLogin();
        if (requestId.isPresent() && requestId.get().equals(userId)) {
            return postRepository.findByAuthorId(userId);
        } else if (requestId.isPresent() && !requestId.get().equals(userId)){
            Boolean isFriend = profileClient.isFriend(requestId.get(), userId).getData();
            if(isFriend) {
                return postRepository.findByAuthorIdAndPrivacyIn(userId, List.of("PUBLIC", "FRIENDS"));
            } else {
                return postRepository.findByAuthorIdAndPrivacyIn(userId, List.of("PUBLIC"));
            }
        }else {
            throw new RuntimeException("Unauthorized access to posts");
        }
    }

    public List<Post> getPostOnMainScreen() {
        Optional<String> requestId = SecurityUtil.getCurrentUserLogin();
        if (requestId.isPresent()) {
            UserProfile friends = profileClient.getFriends(requestId.get());

            List<String> friendIds = new ArrayList<>(
                    friends.getData().stream()
                            .map(UserProfile.UserProfileOne::getUserId)
                            .toList()
            );

            friendIds.add(requestId.get());

            return postRepository.findByAuthorIdInAndPrivacyInOrderByCreatedAtDesc(
                    friendIds,
                    List.of("PUBLIC", "FRIENDS")
            );
        } else {
            return postRepository.findByPrivacyOrderByCreatedAtDesc("PUBLIC");
        }
    }

     public Post updatePost(UpdatePostRequest updatePostRequest) {
        Post post = postRepository.findById(updatePostRequest.getPostId()).orElse(null);
        if (post != null) {
            post.setContent(updatePostRequest.getContent());
            post.setPrivacy(updatePostRequest.getPrivacy());
            post.setMedia(updatePostRequest.getMedia());
            return postRepository.save(post);
        } else {
            throw new RuntimeException("Post not found");
        }
     }

     public boolean deletePost(String postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post != null) {
            postRepository.delete(post);
            return true;
        } else {
            return false;
        }
     }

     public Post unlikePost(LikePostRequest unlikePostRequest) {
         Optional<Post> post = postRepository.findById(unlikePostRequest.getPostId());
         if (post.isPresent()) {
             Post existingPost = post.get();
             List<String> likes = existingPost.getLikes();
             likes.remove(unlikePostRequest.getUserId());
             existingPost.setLikes(likes);
             return postRepository.save(existingPost);
         }
         return null;
     }
}
