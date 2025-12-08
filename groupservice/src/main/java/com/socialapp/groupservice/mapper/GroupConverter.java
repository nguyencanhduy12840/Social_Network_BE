package com.socialapp.groupservice.mapper;

import com.socialapp.groupservice.dto.response.CreateGroupResponse;
import com.socialapp.groupservice.dto.response.GroupDetailResponse;
import com.socialapp.groupservice.entity.Group;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class GroupConverter {

    private final ModelMapper modelMapper;

    public GroupConverter(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public CreateGroupResponse toCreateGroupResponse(Group group) {
        return modelMapper.map(group, CreateGroupResponse.class);
    }

    public GroupDetailResponse toGroupDetailResponse(Group group) {
        return modelMapper.map(group, GroupDetailResponse.class);
    }
}
