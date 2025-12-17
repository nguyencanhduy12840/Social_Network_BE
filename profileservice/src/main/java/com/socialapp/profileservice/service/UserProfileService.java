package com.socialapp.profileservice.service;

import com.socialapp.profileservice.dto.request.ProfileCreationRequest;
import com.socialapp.profileservice.dto.request.UpdateUserProfileRequest;
import com.socialapp.profileservice.dto.response.UserProfileResponse;
import com.socialapp.profileservice.entity.UserProfile;
import com.socialapp.profileservice.mapper.UserProfileConverter;
import com.socialapp.profileservice.repository.UserProfileRepository;
import com.socialapp.profileservice.repository.httpclient.GroupClient;
import com.socialapp.profileservice.repository.httpclient.PostClient;
import com.socialapp.profileservice.util.FriendshipStatus;
import com.socialapp.profileservice.util.SecurityUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final UserProfileConverter userProfileMapper;
    private final FriendshipService friendshipService;
    private final GroupClient groupClient;
    private final PostClient postClient;
    private final CloudinaryService cloudinaryService;

    public UserProfileService(UserProfileRepository userProfileRepository, UserProfileConverter userProfileMapper, FriendshipService friendshipService, CloudinaryService cloudinaryService, GroupClient groupClient, PostClient postClient) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
        this.friendshipService = friendshipService;
        this.cloudinaryService = cloudinaryService;
        this.groupClient = groupClient;
        this.postClient = postClient;
    }

    public UserProfileResponse createProfile(ProfileCreationRequest request) {
        UserProfile userProfile = userProfileMapper.toUserProfile(request);
        userProfile = userProfileRepository.save(userProfile);

        return userProfileMapper.toUserProfileResponse(userProfile);
    }

    public UserProfileResponse getProfile(String id) {
        UserProfile userProfile =
                userProfileRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Profile not found"));

        Optional<String> currentUserId = SecurityUtil.getCurrentUserLogin();

        UserProfileResponse userProfileResponse =
                userProfileMapper.toUserProfileResponse(userProfile);

        if (currentUserId.isPresent() && currentUserId.get().equals(id)) {
            userProfileResponse.setFriendStatus("SELF");
        } else {
            String currentId = currentUserId.orElse(null);
            String friendshipStatus = determineFriendStatus(currentId, id);
            userProfileResponse.setFriendStatus(friendshipStatus);
        }

        userProfileResponse.setFriendCount((int) friendshipService.getFriendCount(id));
        try {
            String userId = userProfile.getUserId();
            log.info("Fetching group count for userId: {}", userId);
            Integer groupCount = groupClient.getGroupCount(userId);
            log.info("Group count for userId {}: {}", userId, groupCount);
            userProfileResponse.setGroupCount(groupCount != null ? groupCount : 0);
        } catch (Exception e) {
            log.error("Error fetching group count for user: " + id, e);
            userProfileResponse.setGroupCount(0);
        }

        try {
            String userId = userProfile.getUserId();
            log.info("Fetching post count for userId: {}", userId);
            Integer postCount = postClient.getPostCount(userId);
            log.info("Post count for userId {}: {}", userId, postCount);
            userProfileResponse.setPostCount(postCount != null ? postCount : 0);
        } catch (Exception e) {
            log.error("Error fetching post count for user: " + id, e);
            userProfileResponse.setPostCount(0);
        }


        return userProfileResponse;
    }

    public List<UserProfileResponse> getAllProfiles() {
        var profiles = userProfileRepository.findAll();

        return profiles.stream().map(userProfileMapper::toUserProfileResponse).toList();
    }

    private String determineFriendStatus(String currentId, String profileId) {

        UserProfile current = userProfileRepository.findByUserId(currentId).orElse(null);
        UserProfile profile = userProfileRepository.findByUserId(profileId).orElse(null);
        if (current == null || profile == null) return "NONE";

        boolean outgoingPending = current.getSentFriendships().stream()
            .anyMatch(f -> f.getFriend().getUserId().equals(profileId)
                        && f.getStatus() == FriendshipStatus.PENDING);

        if (outgoingPending) return "OUTGOING_PENDING";

        boolean incomingPending = current.getReceivedFriendships().stream()
            .anyMatch(f -> f.getFriend().getUserId().equals(profileId)
                        && f.getStatus() == FriendshipStatus.PENDING);

        if (incomingPending) return "INCOMING_PENDING";

        boolean isFriend = current.getSentFriendships().stream().anyMatch(f ->
                        f.getFriend().getUserId().equals(profileId)
                        && f.getStatus() == FriendshipStatus.ACCEPTED)
                        || current.getReceivedFriendships().stream().anyMatch(f ->
                        f.getFriend().getUserId().equals(profileId)
                        && f.getStatus() == FriendshipStatus.ACCEPTED);

        if (isFriend) return "FRIEND";

        return "NONE";
    }

    public UserProfileResponse getProfileById(String id) {
        UserProfile userProfile =
                userProfileRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Profile not found"));

        Optional<String> currentUserId = SecurityUtil.getCurrentUserLogin();

        UserProfileResponse userProfileResponse =
                userProfileMapper.toUserProfileResponse(userProfile);

        if (currentUserId.isPresent() && currentUserId.get().equals(id)) {
            userProfileResponse.setFriendStatus("SELF");
        } else {
            String currentId = currentUserId.orElse(null);
            String friendshipStatus = determineFriendStatus(currentId, id);
            userProfileResponse.setFriendStatus(friendshipStatus);
        }

        return userProfileResponse;
    }

    public UserProfile updateProfile(UpdateUserProfileRequest request, MultipartFile mediaFile){
        UserProfile userProfile =
                userProfileRepository.findByUserId(request.getUserId())
                        .orElseThrow(() -> new RuntimeException("Profile not found"));

        String mediaUrls="";
        if ( mediaFile!= null) {
            String url = cloudinaryService.uploadImage(mediaFile);
            mediaUrls += url;
        }

        if(request.getFirstName() != null){
            userProfile.setFirstName(request.getFirstName());
        }
        if(request.getLastName() != null){
            userProfile.setLastName(request.getLastName());
        }
        if(request.getUsername() != null){
            userProfile.setUsername(request.getUsername());
        }
        if(request.getBio() != null){
            userProfile.setBio(request.getBio());
        }
        if(request.getGender() != null){
            userProfile.setGender(request.getGender());
        }
        if (request.getDob() != null) {
            userProfile.setDob(request.getDob());
        }

        userProfile.setAvatarUrl(mediaUrls);
        return userProfileRepository.save(userProfile);
    }

    public List<UserProfileResponse> searchUsersByUsername(String keyword, int page, int size) {
        long skip = (long) page * size;
        List<UserProfile> users = userProfileRepository.searchByUsername(keyword, skip, size);

        Optional<String> currentUserId = SecurityUtil.getCurrentUserLogin();
        String currentId = currentUserId.orElse(null);

        return users.stream()
                .map(user -> {
                    UserProfileResponse response = userProfileMapper.toUserProfileResponse(user);

                    if (currentId != null && currentId.equals(user.getId())) {
                        response.setFriendStatus("SELF");
                    } else {
                        String friendshipStatus = determineFriendStatus(currentId, user.getId());
                        response.setFriendStatus(friendshipStatus);
                    }

                    response.setFriendCount((int) friendshipService.getFriendCount(user.getId()));

                    return response;
                })
                .toList();
    }
}
