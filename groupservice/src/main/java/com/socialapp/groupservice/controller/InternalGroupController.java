package com.socialapp.groupservice.controller;

import com.socialapp.groupservice.dto.response.ApiResponse;
import com.socialapp.groupservice.dto.response.GroupMemberResponse;
import com.socialapp.groupservice.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal")
public class InternalGroupController {

    private final GroupService groupService;

    public InternalGroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> getGroupMembersInternal(@PathVariable String groupId) {
        List<GroupMemberResponse> members = groupService.getGroupMembersInternal(groupId);
        ApiResponse<List<GroupMemberResponse>> response = ApiResponse.<List<GroupMemberResponse>>builder()
                .data(members)
                .build();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{groupId}/is-member")
    public ResponseEntity<Boolean> isGroupMember(@PathVariable String groupId, @RequestParam String userId) {
        return ResponseEntity.ok(groupService.isGroupMember(groupId, userId));
    }

    @GetMapping("/{groupId}/privacy")
    public ResponseEntity<String> getGroupPrivacy(@PathVariable String groupId) {
        return ResponseEntity.ok(groupService.getGroupPrivacyInternal(groupId));
    }
    
    @GetMapping("/count")
    public ResponseEntity<Integer> getGroupCountByUserId(@RequestParam String userId) {
        return ResponseEntity.ok(groupService.getGroupCountByUserId(userId));
    }
}
