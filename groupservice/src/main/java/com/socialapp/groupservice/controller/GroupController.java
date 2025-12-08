package com.socialapp.groupservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialapp.groupservice.dto.request.CreateGroupRequest;
import com.socialapp.groupservice.dto.request.UpdateMemberRoleRequest;
import com.socialapp.groupservice.dto.response.*;
import com.socialapp.groupservice.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateGroupResponse> createGroup(
            @RequestPart("group") String request,
            @RequestPart(value = "backgroundImage", required = false) MultipartFile backgroundImage) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        CreateGroupRequest createGroupRequest = mapper.readValue(request, CreateGroupRequest.class);
        CreateGroupResponse response = groupService.createGroup(createGroupRequest, backgroundImage);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailResponse> getGroupDetail(@PathVariable String groupId) {
        GroupDetailResponse response = groupService.getGroupDetail(groupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<JoinGroupResponse> joinGroup(@PathVariable String groupId) {
        JoinGroupResponse response = groupService.joinGroup(groupId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupId}/leave")
    public ResponseEntity<LeaveGroupResponse> leaveGroup(@PathVariable String groupId) {
        LeaveGroupResponse response = groupService.leaveGroup(groupId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupId}/join-requests/{requestId}/handle")
    public ResponseEntity<HandleJoinRequestResponse> handleJoinRequest(
            @PathVariable String groupId,
            @PathVariable String requestId,
            @RequestParam Boolean approved) {
        HandleJoinRequestResponse response = groupService.handleJoinRequest(groupId, requestId, approved);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> getGroupMembers(@PathVariable String groupId) {
        List<GroupMemberResponse> response = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{groupId}/members/{memberId}/role")
    public ResponseEntity<GroupMemberResponse> updateMemberRole(
            @PathVariable String groupId,
            @PathVariable String memberId,
            @RequestBody UpdateMemberRoleRequest request) {
        GroupMemberResponse response = groupService.updateMemberRole(groupId, memberId, request.getRole());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<RemoveMemberResponse> removeMember(
            @PathVariable String groupId,
            @PathVariable String memberId) {
        RemoveMemberResponse response = groupService.removeMember(groupId, memberId);
        return ResponseEntity.ok(response);
    }
}
