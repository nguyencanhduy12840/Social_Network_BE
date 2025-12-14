package com.socialapp.groupservice.mapper;

import com.socialapp.groupservice.dto.response.GroupDetailResponse;
import com.socialapp.groupservice.dto.response.GroupResponse;
import com.socialapp.groupservice.entity.Group;
import com.socialapp.groupservice.repository.GroupMemberRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class GroupConverter {

    private final ModelMapper modelMapper;
    private final GroupMemberRepository groupMemberRepository;

    public GroupConverter(ModelMapper modelMapper, GroupMemberRepository groupMemberRepository) {
        this.modelMapper = modelMapper;
        this.groupMemberRepository = groupMemberRepository;
    }

    public GroupResponse toGroupResponse(Group group) {
        Integer memberCount = groupMemberRepository.countMembersByGroupId(group.getId());
        return new GroupResponse(
            group.getId(),
            group.getName(),
            group.getDescription(),
            group.getAvatarUrl(),
            memberCount != null ? memberCount : 0,
            group.getPrivacy()
        );
    }

    public GroupDetailResponse toGroupDetailResponse(Group group) {
        return modelMapper.map(group, GroupDetailResponse.class);
    }
}
