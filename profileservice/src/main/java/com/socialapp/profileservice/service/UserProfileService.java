package com.socialapp.profileservice.service;

import com.socialapp.profileservice.dto.request.ProfileCreationRequest;
import com.socialapp.profileservice.dto.response.UserProfileResponse;
import com.socialapp.profileservice.entity.UserProfile;
import com.socialapp.profileservice.mapper.UserProfileConverter;
import com.socialapp.profileservice.repository.UserProfileRepository;
import com.socialapp.profileservice.repository.httpclient.PostClient;
import com.socialapp.profileservice.util.FriendshipStatus;
import com.socialapp.profileservice.util.SecurityUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final UserProfileConverter userProfileMapper;
    private final FriendshipService friendshipService;
    private final PostClient postClient;

    public UserProfileService(UserProfileRepository userProfileRepository, UserProfileConverter userProfileMapper, FriendshipService friendshipService, PostClient postClient) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
        this.friendshipService = friendshipService;
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

        userProfileResponse.setPosts(postClient.getPosts(id).getData());
        userProfileResponse.setFriendships(friendshipService.getFriends(id, 0, 100));

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
}
