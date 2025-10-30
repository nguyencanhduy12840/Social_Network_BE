package com.socialapp.profileservice.mapper;

import com.socialapp.profileservice.dto.request.ProfileCreationRequest;
import com.socialapp.profileservice.dto.response.UserProfileResponse;
import com.socialapp.profileservice.entity.UserProfile;
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
        return modelMapper.map(userProfile, UserProfileResponse.class);
    }
}
