package com.socialapp.postservice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import com.socialapp.postservice.dto.request.SeenPostRequest;
import com.socialapp.postservice.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.socialapp.postservice.dto.request.BaseEvent;
import com.socialapp.postservice.dto.request.LikePostRequest;
import com.socialapp.postservice.dto.request.PostEvent;
import com.socialapp.postservice.entity.Post;
import com.socialapp.postservice.mapper.PostConverter;
import com.socialapp.postservice.repository.PostRepository;
import com.socialapp.postservice.repository.httpclient.ProfileClient;
import com.socialapp.postservice.repository.httpclient.GroupClient;
import com.socialapp.postservice.util.SecurityUtil;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostConverter postConverter;
    private final CloudinaryService cloudinaryService;

    private final KafkaTemplate<String, BaseEvent> kafkaTemplate;
    private final ProfileClient profileClient;
    private final GroupClient groupClient;

    private final String NOTIFICATION_TOPIC = "notification-events";

    public PostService(PostRepository postRepository, PostConverter postConverter,
                       CloudinaryService cloudinaryService, KafkaTemplate<String, BaseEvent> kafkaTemplate,
                       ProfileClient profileClient, GroupClient groupClient) {
        this.postRepository = postRepository;
        this.postConverter = postConverter;
        this.cloudinaryService = cloudinaryService;
        this.kafkaTemplate = kafkaTemplate;
        this.profileClient = profileClient;
        this.groupClient = groupClient;
    }

    public CreatePostResponse createPost(String userId, String content, String groupId, String privacy, String type, MultipartFile[] mediaFiles) {
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

        if (groupId != null && !groupId.isEmpty()) {
             Boolean isMember = groupClient.isGroupMember(groupId, userId);
             if (!Boolean.TRUE.equals(isMember)) {
                 throw new RuntimeException("You are not a member of this group");
             }
        }

        Post post = Post.builder()
                .authorId(userId)
                .groupId(groupId)
                .type(type)
                .seenBy(new ArrayList<>())
                .content(content)
                .privacy(privacy)
                .media(mediaUrls)
                .createdAt(Instant.now())
                .likes(new ArrayList<>())
                .commentsCount(0)
                .build();

        Post savedPost = postRepository.save(post);

        // Xử lý gửi thông báo
        if (groupId != null && !groupId.isEmpty()) {
            // TH1: Post trong group - gửi thông báo cho thành viên group
            try {
                ApiResponse<List<GroupMemberResponse>> groupMembersResponse = groupClient.getGroupMembers(groupId);
                List<GroupMemberResponse> groupMembers = groupMembersResponse.getData();

                if (groupMembers != null && !groupMembers.isEmpty()) {
                    groupMembers.parallelStream()
                        .filter(member -> !member.getUserId().equals(userId)) // Không gửi cho chính tác giả
                        .forEach(member -> {
                            PostEvent event = PostEvent.builder()
                                    .postId(savedPost.getId())
                                    .authorId(userId)
                                    .groupId(groupId)
                                    .eventType("NEW_POST_IN_GROUP")
                                    .receiverId(member.getUserId())
                                    .build();

                            BaseEvent baseEvent = BaseEvent.builder()
                                    .eventType("NEW_POST_IN_GROUP")
                                    .sourceService("PostService")
                                    .payload(event)
                                    .build();

                            kafkaTemplate.send(NOTIFICATION_TOPIC, baseEvent);
                        });
                }
            } catch (Exception e) {
                // Log lỗi nhưng không ảnh hưởng việc tạo post
                System.err.println("Error sending notifications to group members: " + e.getMessage());
            }
        } else if (savedPost.getPrivacy().equals("PUBLIC") || savedPost.getPrivacy().equals("FRIENDS")) {
            // TH2: Post cá nhân (không có groupId) - gửi thông báo cho bạn bè
            try {
                UserProfile friends = profileClient.getFriends(userId);
                if (friends != null && friends.getData() != null) {
                    friends.getData().parallelStream().forEach(friend -> {
                        PostEvent event = PostEvent.builder()
                                .postId(savedPost.getId())
                                .authorId(userId)
                                .groupId("")
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
            } catch (Exception e) {
                // Log lỗi nhưng không ảnh hưởng việc tạo post
                System.err.println("Error sending notifications to friends: " + e.getMessage());
            }
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

    public PagedPostResponse getPostOnMainScreen(int page, int size, String type) {
        Optional<String> requestId = SecurityUtil.getCurrentUserLogin();
        Pageable pageable = PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"
                ));

        Page<Post> postsPage;

        if (requestId.isPresent()) {
            String currentUserId = requestId.get();

            // Lấy danh sách bạn bè
            UserProfile friends = profileClient.getFriends(currentUserId);
            List<String> friendIds = friends.getData().stream()
                    .map(UserProfile.UserProfileOne::getUserId)
                    .toList();

            // Sử dụng custom repository với MongoDB query - tối ưu hiệu năng
            postsPage = postRepository.findPostsForMainScreen(currentUserId, friendIds, type, pageable);
        } else {
            // Người dùng chưa đăng nhập - chỉ thấy PUBLIC
            postsPage = postRepository.findByPrivacyAndTypeOrderByCreatedAtDesc("PUBLIC", type, pageable);
        }

        // Collect all unique author IDs
        List<String> authorIds = postsPage.getContent().stream()
                .map(Post::getAuthorId)
                .distinct()
                .toList();

        // Fetch all author profiles in one batch call
        java.util.Map<String, OneUserProfileResponse.UserProfileOne> authorProfiles = new java.util.HashMap<>();
        if (!authorIds.isEmpty()) {
            try {
                authorProfiles = profileClient.getUserProfiles(authorIds);
            } catch (Exception e) {
                System.err.println("Error fetching author profiles: " + e.getMessage());
            }
        }

        // Convert posts to response
        List<PostResponse> postResponses = new ArrayList<>();
        for (Post post : postsPage.getContent()) {
            PostResponse postResponse = postConverter.convertToPostResponse(post);
            
            // Get author profile from the batch result
            OneUserProfileResponse.UserProfileOne authorProfile = authorProfiles.get(post.getAuthorId());
            if (authorProfile != null) {
                postResponse.setAuthorProfile(authorProfile);
            }
            
            postResponses.add(postResponse);
        }

        // Tạo PagedPostResponse với thông tin pagination đầy đủ
        return PagedPostResponse.builder()
                .posts(postResponses)
                .currentPage(postsPage.getNumber())
                .totalPages(postsPage.getTotalPages())
                .totalElements(postsPage.getTotalElements())
                .pageSize(postsPage.getSize())
                .hasNext(postsPage.hasNext())
                .hasPrevious(postsPage.hasPrevious())
                .build();
    }

     public Post updatePost(String postId, String content, String privacy, MultipartFile[] mediaFiles) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post != null) {
            post.setContent(content);
            List<String> mediaUrls = new ArrayList<>();
            post.setPrivacy(privacy);
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
            post.setMedia(mediaUrls);
            post.setUpdatedAt(Instant.now());
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

     public PostResponse getPostById(String postId){
        Optional<Post> post = postRepository.findById(postId);
        if(post.isPresent()){
            Post currentPost = post.get();
            PostResponse postResponse = postConverter.convertToPostResponse(currentPost);
            OneUserProfileResponse authorProfile = profileClient.getUserProfile(currentPost.getAuthorId());
            postResponse.setAuthorProfile(authorProfile.getData());
            return postResponse;
        }
        return null;
     }

     public List<OneUserProfileResponse.UserProfileOne> getUserLikePost(String postId){
        Optional<Post> post = postRepository.findById(postId);
        if(post.isPresent()){
            Post currentPost = post.get();
            List<String> likeUserIds = currentPost.getLikes();
            
            // Fetch all user profiles in one batch call
            java.util.Map<String, OneUserProfileResponse.UserProfileOne> userProfiles = new java.util.HashMap<>();
            if (!likeUserIds.isEmpty()) {
                try {
                    userProfiles = profileClient.getUserProfiles(likeUserIds);
                } catch (Exception e) {
                    System.err.println("Error fetching user profiles: " + e.getMessage());
                }
            }
            
            // Convert to list maintaining order
            List<OneUserProfileResponse.UserProfileOne> likeUsers = new ArrayList<>();
            for(String userId : likeUserIds){
                OneUserProfileResponse.UserProfileOne userProfile = userProfiles.get(userId);
                if (userProfile != null) {
                    likeUsers.add(userProfile);
                }
            }
            return likeUsers;
        }
        return null;
     }

     public List<Post> markPostsAsSeen(SeenPostRequest seenPostRequest) {
        List<String> postIds = seenPostRequest.getPostIds();
        String viewerId = seenPostRequest.getViewerId();
        
        if (postIds == null || postIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Post> posts = postRepository.findAllById(postIds);
        List<Post> updatedPosts = new ArrayList<>();
        
        for (Post post : posts) {
            List<String> seenBy = post.getSeenBy();
            if (!seenBy.contains(viewerId)) {
                seenBy.add(viewerId);
                post.setSeenBy(seenBy);
                updatedPosts.add(post);
            }
        }
        
        if (!updatedPosts.isEmpty()) {
            return postRepository.saveAll(updatedPosts);
        }
        
        return updatedPosts;
     }

     public List<OneUserProfileResponse.UserProfileOne> getUsersWhoSeenPost(String postId) {
         Optional<Post> post = postRepository.findById(postId);
         if (post.isPresent()) {
             Post currentPost = post.get();
             List<String> seenUserIds = currentPost.getSeenBy();
             
             // Fetch all user profiles in one batch call
             java.util.Map<String, OneUserProfileResponse.UserProfileOne> userProfiles = new java.util.HashMap<>();
             if (!seenUserIds.isEmpty()) {
                 try {
                     userProfiles = profileClient.getUserProfiles(seenUserIds);
                 } catch (Exception e) {
                     System.err.println("Error fetching user profiles: " + e.getMessage());
                 }
             }
             
             // Convert to list maintaining order
             List<OneUserProfileResponse.UserProfileOne> seenUsers = new ArrayList<>();
             for (String userId : seenUserIds) {
                 OneUserProfileResponse.UserProfileOne userProfile = userProfiles.get(userId);
                 if (userProfile != null) {
                     seenUsers.add(userProfile);
                 }
             }
             return seenUsers;
         }
         return null;
     }
        
    public PagedPostResponse getGroupPosts(String groupId, int page, int size) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Check group privacy
        String privacy = groupClient.getGroupPrivacy(groupId);
        if (privacy == null) {
            throw new RuntimeException("Group not found");
        }

        boolean isMember = Boolean.TRUE.equals(groupClient.isGroupMember(groupId, currentUserId));

        if ("PRIVATE".equalsIgnoreCase(privacy) && !isMember) {
            throw new RuntimeException("Access denied. This is a private group.");
        }

        Pageable pageable = PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"
                ));

        Page<Post> postsPage = postRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId, pageable);

        // Collect all unique author IDs
        List<String> authorIds = postsPage.getContent().stream()
                .map(Post::getAuthorId)
                .distinct()
                .toList();

        // Fetch all author profiles in one batch call
        java.util.Map<String, OneUserProfileResponse.UserProfileOne> authorProfiles = new java.util.HashMap<>();
        if (!authorIds.isEmpty()) {
            try {
                authorProfiles = profileClient.getUserProfiles(authorIds);
            } catch (Exception e) {
                System.err.println("Error fetching author profiles: " + e.getMessage());
            }
        }

        // Convert posts to response
        List<PostResponse> postResponses = new ArrayList<>();
        for (Post post : postsPage.getContent()) {
            PostResponse postResponse = postConverter.convertToPostResponse(post);
            
            // Get author profile from the batch result
            OneUserProfileResponse.UserProfileOne authorProfile = authorProfiles.get(post.getAuthorId());
            if (authorProfile != null) {
                postResponse.setAuthorProfile(authorProfile);
            }
            
            postResponses.add(postResponse);
        }

        return PagedPostResponse.builder()
                .posts(postResponses)
                .currentPage(postsPage.getNumber())
                .totalPages(postsPage.getTotalPages())
                .totalElements(postsPage.getTotalElements())
                .pageSize(postsPage.getSize())
                .hasNext(postsPage.hasNext())
                .hasPrevious(postsPage.hasPrevious())
                .build();
    }

    public PagedPostResponse getUserPosts(String userId, int page, int size, String type) {
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        Pageable pageable = PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        Page<Post> postsPage;

        // If requesting own posts, return all posts
        if (currentUserId.equals(userId)) {
            postsPage = postRepository.findByAuthorIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        } else {
            // Check if users are friends
            Boolean isFriend = profileClient.isFriend(currentUserId, userId).getData();
            if (Boolean.TRUE.equals(isFriend)) {
                // Return PUBLIC and FRIENDS posts
                postsPage = postRepository.findByAuthorIdAndTypeAndPrivacyInOrderByCreatedAtDesc(
                        userId, type, List.of("PUBLIC", "FRIENDS"), pageable);
            } else {
                // Return only PUBLIC posts
                postsPage = postRepository.findByAuthorIdAndTypeAndPrivacyInOrderByCreatedAtDesc(
                        userId, type, List.of("PUBLIC"), pageable);
            }
        }

        // Collect all unique author IDs
        List<String> authorIds = postsPage.getContent().stream()
                .map(Post::getAuthorId)
                .distinct()
                .toList();

        // Fetch all author profiles in one batch call
        java.util.Map<String, OneUserProfileResponse.UserProfileOne> authorProfiles = new java.util.HashMap<>();
        if (!authorIds.isEmpty()) {
            try {
                authorProfiles = profileClient.getUserProfiles(authorIds);
            } catch (Exception e) {
                System.err.println("Error fetching author profiles: " + e.getMessage());
            }
        }

        // Convert posts to response
        List<PostResponse> postResponses = new ArrayList<>();
        for (Post post : postsPage.getContent()) {
            PostResponse postResponse = postConverter.convertToPostResponse(post);
            
            // Get author profile from the batch result
            OneUserProfileResponse.UserProfileOne authorProfile = authorProfiles.get(post.getAuthorId());
            if (authorProfile != null) {
                postResponse.setAuthorProfile(authorProfile);
            }
            
            postResponses.add(postResponse);
        }

        return PagedPostResponse.builder()
                .posts(postResponses)
                .currentPage(postsPage.getNumber())
                .totalPages(postsPage.getTotalPages())
                .totalElements(postsPage.getTotalElements())
                .pageSize(postsPage.getSize())
                .hasNext(postsPage.hasNext())
                .hasPrevious(postsPage.hasPrevious())
                .build();
    }

    @Transactional(readOnly = true)
    public int countPostsByUserId(String userId) {
        return postRepository.countByAuthorId(userId);
    }
}
