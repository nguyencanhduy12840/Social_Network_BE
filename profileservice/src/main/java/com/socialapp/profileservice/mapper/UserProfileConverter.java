package com.socialapp.profileservice.mapper;

import com.socialapp.profileservice.dto.request.ProfileCreationRequest;
import com.socialapp.profileservice.dto.response.UserProfileResponse;
import com.socialapp.profileservice.entity.UserProfile;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UserProfileConverter {

    private final ModelMapper modelMapper;

    public UserProfileConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    // ✅ Chặn lỗi ambiguity trong ModelMapper cũ
    @PostConstruct
    public void setup() {
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
    }

    public UserProfile toUserProfile(ProfileCreationRequest request) {
        return modelMapper.map(request, UserProfile.class);
    }

    public UserProfileResponse toUserProfileResponse(UserProfile userProfile) {
        UserProfileResponse response = modelMapper.map(userProfile, UserProfileResponse.class);

        return response;
    }
}
