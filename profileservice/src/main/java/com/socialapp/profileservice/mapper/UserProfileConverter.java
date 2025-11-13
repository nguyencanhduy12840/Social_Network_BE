package com.socialapp.profileservice.mapper;

import com.socialapp.profileservice.dto.request.ProfileCreationRequest;
import com.socialapp.profileservice.dto.response.UserProfileResponse;
import com.socialapp.profileservice.entity.Friendship;
import com.socialapp.profileservice.entity.UserProfile;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UserProfileConverter {

    private final ModelMapper modelMapper;
    public UserProfileConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public UserProfile toUserProfile(ProfileCreationRequest request) {
        return modelMapper.map(request, UserProfile.class);
    }

    public UserProfileResponse toUserProfileResponse(UserProfile userProfile) {
        UserProfileResponse response = modelMapper.map(userProfile, UserProfileResponse.class);

        List<UserProfile> allFriends = new ArrayList<>();

        if (userProfile.getSentFriendships() != null) {
            userProfile.getSentFriendships().forEach(friendship -> {
                if (friendship.getFriend() != null) {
                    allFriends.add(friendship.getFriend());
                }
            });
        }

        if (userProfile.getReceivedFriendships() != null) {
            userProfile.getReceivedFriendships().forEach(friendship -> {
                if (friendship.getFriend() != null) {
                    allFriends.add(friendship.getFriend());
                }
            });
        }

        response.setFriendships(allFriends);
        return response;
    }
}
