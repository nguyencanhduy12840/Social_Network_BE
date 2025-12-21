package com.socialapp.postservice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public CreatePostResponse createPost(String userId, String content, String groupId, String privacy, String type,
            MultipartFile[] mediaFiles) {
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
                            .filter(member -> member.getUser() != null && !member.getUser().getId().equals(userId))
                            .forEach(member -> {
                                PostEvent event = PostEvent.builder()
                                        .postId(savedPost.getId())
                                        .authorId(userId)
                                        .groupId(groupId)
                                        .eventType("GROUP_NEW_POST")
                                        .receiverId(member.getUser().getId())
                                        .build();

                                BaseEvent baseEvent = BaseEvent.builder()
                                        .eventType("GROUP_NEW_POST")
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
                    // Determine event type based on post type
                    boolean isStory = "STORY".equals(type);
                    String eventType = isStory ? "NEW_STORY" : "NEW_POST";

                    friends.getData().parallelStream().forEach(friend -> {
                        PostEvent event = PostEvent.builder()
                                .postId(isStory ? null : savedPost.getId())
                                .storyId(isStory ? savedPost.getId() : null)
                                .authorId(userId)
                                .groupId("")
                                .eventType(eventType)
                                .receiverId(friend.getId())
                                .build();

                        BaseEvent baseEvent = BaseEvent.builder()
                                .eventType(eventType)
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
        if (post.isPresent()) {
            Post existingPost = post.get();
            List<String> likes = existingPost.getLikes();
            boolean isLiked = false;

            if (likes.contains(likePostRequest.getUserId())) {
                likes.remove(likePostRequest.getUserId());
                isLiked = true;
            } else {
                likes.add(likePostRequest.getUserId());
            }
            existingPost.setLikes(likes);
            Post savedPost = postRepository.save(existingPost);

            // Gửi notification khi like (không gửi khi unlike)
            if (!isLiked && !likePostRequest.getUserId().equals(savedPost.getAuthorId())) {
                // Determine event type based on post type
                boolean isStory = "STORY".equals(savedPost.getType());
                String likeEventType = isStory ? "LIKE_STORY" : "LIKE_POST";

                PostEvent postEvent = PostEvent.builder()
                        .postId(isStory ? null : savedPost.getId())
                        .storyId(isStory ? savedPost.getId() : null)
                        .authorId(likePostRequest.getUserId())
                        .groupId(savedPost.getGroupId() != null ? savedPost.getGroupId() : "")
                        .eventType(likeEventType)
                        .receiverId(savedPost.getAuthorId())
                        .build();

                BaseEvent baseEvent = BaseEvent.builder()
                        .eventType(likeEventType)
                        .sourceService("PostService")
                        .payload(postEvent)
                        .build();

                kafkaTemplate.send(NOTIFICATION_TOPIC, baseEvent);
            }

            return savedPost;
        }
        return null;
    }

    public List<Post> getPostsByUserId(String userId) {
        Optional<String> requestId = SecurityUtil.getCurrentUserLogin();
        if (requestId.isPresent() && requestId.get().equals(userId)) {
            return postRepository.findByAuthorId(userId);
        } else if (requestId.isPresent() && !requestId.get().equals(userId)) {
            Boolean isFriend = profileClient.isFriend(requestId.get(), userId).getData();
            if (isFriend) {
                return postRepository.findByAuthorIdAndPrivacyIn(userId, List.of("PUBLIC", "FRIENDS"));
            } else {
                return postRepository.findByAuthorIdAndPrivacyIn(userId, List.of("PUBLIC"));
            }
        } else {
            throw new RuntimeException("Unauthorized access to posts");
        }
    }

    public PagedPostResponse getPostOnMainScreen(int page, int size, String type) {
        Optional<String> requestId = SecurityUtil.getCurrentUserLogin();
        Pageable pageable = PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        Page<Post> postsPage;

        if (requestId.isPresent()) {
            String currentUserId = requestId.get();

            // Lấy danh sách bạn bè
            UserProfile friends = profileClient.getFriends(currentUserId);
            List<String> friendIds = friends.getData().stream()
                    .map(UserProfile.UserProfileOne::getId)
                    .toList();

            // Sử dụng custom repository với MongoDB query - tối ưu hiệu năng
            postsPage = postRepository.findPostsForMainScreen(currentUserId, friendIds, type, pageable);
        } else {
            // Người dùng chưa đăng nhập - chỉ thấy PUBLIC
            postsPage = postRepository.findByPrivacyAndTypeOrderByCreatedAtDesc("PUBLIC", type, pageable);
        }

        // Collect UNIQUE author IDs
        List<String> uniqueAuthorIds = postsPage.getContent().stream()
                .map(Post::getAuthorId)
                .distinct()
                .toList();

        // Fetch profiles for UNIQUE IDs and cache in map
        Map<String, UserProfile.UserProfileOne> profileCache = new java.util.HashMap<>();
        for (String authorId : uniqueAuthorIds) {
            try {
                OneUserProfileResponse response = profileClient.getUserProfile(authorId);
                if (response != null && response.getData() != null) {
                    // Convert to simplified UserProfile.UserProfileOne
                    UserProfile.UserProfileOne simpleProfile = new UserProfile.UserProfileOne();
                    simpleProfile.setId(response.getData().getId());
                    simpleProfile.setUsername(response.getData().getUsername());
                    simpleProfile.setAvatarUrl(response.getData().getAvatarUrl());
                    profileCache.put(authorId, simpleProfile);
                }
            } catch (Exception e) {
                System.err.println("Error fetching profile for " + authorId + ": " + e.getMessage());
            }
        }

        // Convert posts to response using cached profiles
        List<PostResponse> postResponses = new ArrayList<>();
        for (Post post : postsPage.getContent()) {
            PostResponse postResponse = postConverter.convertToPostResponse(post);
            UserProfile.UserProfileOne profile = profileCache.get(post.getAuthorId());
            if (profile != null) {
                postResponse.setAuthorProfile(profile);
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

    public PostResponse getPostById(String postId) {
        Optional<Post> post = postRepository.findById(postId);
        if (post.isPresent()) {
            Post currentPost = post.get();
            PostResponse postResponse = postConverter.convertToPostResponse(currentPost);
            OneUserProfileResponse authorProfile = profileClient.getUserProfile(currentPost.getAuthorId());
            if (authorProfile != null && authorProfile.getData() != null) {
                UserProfile.UserProfileOne simpleProfile = new UserProfile.UserProfileOne();
                simpleProfile.setId(authorProfile.getData().getId());
                simpleProfile.setUsername(authorProfile.getData().getUsername());
                simpleProfile.setAvatarUrl(authorProfile.getData().getAvatarUrl());
                postResponse.setAuthorProfile(simpleProfile);
            }
            return postResponse;
        }
        return null;
    }

    public List<OneUserProfileResponse.UserProfileOne> getUserLikePost(String postId) {
        Optional<Post> post = postRepository.findById(postId);
        if (post.isPresent()) {
            Post currentPost = post.get();
            List<String> likeUserIds = currentPost.getLikes();
            List<OneUserProfileResponse.UserProfileOne> likeUsers = new ArrayList<>();
            for (String userId : likeUserIds) {
                OneUserProfileResponse authorProfile = profileClient.getUserProfile(userId);
                likeUsers.add(authorProfile.getData());
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
            List<OneUserProfileResponse.UserProfileOne> seenUsers = new ArrayList<>();
            for (String userId : seenUserIds) {
                OneUserProfileResponse authorProfile = profileClient.getUserProfile(userId);
                seenUsers.add(authorProfile.getData());
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
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        Page<Post> postsPage = postRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId, pageable);

        // Convert posts to response
        List<PostResponse> postResponses = new ArrayList<>();
        for (Post post : postsPage.getContent()) {
            PostResponse postResponse = postConverter.convertToPostResponse(post);
            OneUserProfileResponse authorProfile = profileClient.getUserProfile(post.getAuthorId());
            if (authorProfile != null && authorProfile.getData() != null) {
                UserProfile.UserProfileOne simpleProfile = new UserProfile.UserProfileOne();
                simpleProfile.setId(authorProfile.getData().getId());
                simpleProfile.setUsername(authorProfile.getData().getUsername());
                simpleProfile.setAvatarUrl(authorProfile.getData().getAvatarUrl());
                postResponse.setAuthorProfile(simpleProfile);
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
            postsPage = postRepository.findByAuthorIdAndTypeAndGroupIdIsNullOrderByCreatedAtDesc(userId, type,
                    pageable);
        } else {
            // Check if users are friends
            Boolean isFriend = profileClient.isFriend(currentUserId, userId).getData();
            if (Boolean.TRUE.equals(isFriend)) {
                // Return PUBLIC and FRIENDS posts (excluding group posts)
                postsPage = postRepository.findByAuthorIdAndTypeAndPrivacyInAndGroupIdIsNullOrderByCreatedAtDesc(
                        userId, type, List.of("PUBLIC", "FRIENDS"), pageable);
            } else {
                // Return only PUBLIC posts (excluding group posts)
                postsPage = postRepository.findByAuthorIdAndTypeAndPrivacyInAndGroupIdIsNullOrderByCreatedAtDesc(
                        userId, type, List.of("PUBLIC"), pageable);
            }
        }

        // Convert posts to response
        List<PostResponse> postResponses = new ArrayList<>();
        for (Post post : postsPage.getContent()) {
            PostResponse postResponse = postConverter.convertToPostResponse(post);
            OneUserProfileResponse authorProfile = profileClient.getUserProfile(post.getAuthorId());
            if (authorProfile != null && authorProfile.getData() != null) {
                UserProfile.UserProfileOne simpleProfile = new UserProfile.UserProfileOne();
                simpleProfile.setId(authorProfile.getData().getId());
                simpleProfile.setUsername(authorProfile.getData().getUsername());
                simpleProfile.setAvatarUrl(authorProfile.getData().getAvatarUrl());
                postResponse.setAuthorProfile(simpleProfile);
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

    // Methods for group operations
    @Transactional
    public void deletePostsByGroupId(String groupId) {
        postRepository.deleteAllByGroupId(groupId);
    }

    @Transactional
    public void deletePostsByGroupIdAndAuthorId(String groupId, String authorId) {
        postRepository.deleteAllByGroupIdAndAuthorId(groupId, authorId);
    }

    public List<PostResponse> searchPosts(String keyword) {
        // Search posts with type POST, group null, and content contains keyword
        List<Post> posts = postRepository
                .findByTypeAndGroupIdIsNullAndContentContainingIgnoreCaseOrderByCreatedAtDesc(
                        "POST", keyword);

        // Collect UNIQUE author IDs
        List<String> uniqueAuthorIds = posts.stream()
                .map(Post::getAuthorId)
                .distinct()
                .toList();

        // Fetch profiles for UNIQUE IDs and cache in map
        Map<String, UserProfile.UserProfileOne> profileCache = new java.util.HashMap<>();
        for (String authorId : uniqueAuthorIds) {
            try {
                OneUserProfileResponse response = profileClient.getUserProfile(authorId);
                if (response != null && response.getData() != null) {
                    UserProfile.UserProfileOne simpleProfile = new UserProfile.UserProfileOne();
                    simpleProfile.setId(response.getData().getId());
                    simpleProfile.setUsername(response.getData().getUsername());
                    simpleProfile.setAvatarUrl(response.getData().getAvatarUrl());
                    profileCache.put(authorId, simpleProfile);
                }
            } catch (Exception e) {
                System.err.println("Error fetching profile for " + authorId + ": " + e.getMessage());
            }
        }

        // Convert posts to response using cached profiles
        List<PostResponse> postResponses = new ArrayList<>();
        for (Post post : posts) {
            PostResponse postResponse = postConverter.convertToPostResponse(post);
            UserProfile.UserProfileOne profile = profileCache.get(post.getAuthorId());
            if (profile != null) {
                postResponse.setAuthorProfile(profile);
            }
            postResponses.add(postResponse);
        }

        return postResponses;
    }
}
