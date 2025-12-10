package com.socialapp.groupservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialapp.groupservice.dto.request.*;
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
            @RequestPart(value = "background", required = false) MultipartFile background,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        CreateGroupRequest createGroupRequest = mapper.readValue(request, CreateGroupRequest.class);
        CreateGroupResponse response = groupService.createGroup(createGroupRequest, background, avatar);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailResponse> getGroupDetail(@PathVariable String groupId) {
        GroupDetailResponse response = groupService.getGroupDetail(groupId);
        return ResponseEntity.ok(response);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UpdateGroupResponse> updateGroup(
            @RequestPart("group") String request,
            @RequestPart(value = "background", required = false) MultipartFile background,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        UpdateGroupRequest updateGroupRequest = mapper.readValue(request, UpdateGroupRequest.class);
        UpdateGroupResponse response = groupService.updateGroup(updateGroupRequest, background, avatar);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String groupId) {
        groupService.deleteGroup(groupId);
        return ResponseEntity.ok().build();
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

    @PutMapping("/requests")
    public ResponseEntity<HandleJoinRequestResponse> handleJoinRequest(
            @RequestBody HandleJoinRequestRequest request) {
        HandleJoinRequestResponse response = groupService.handleJoinRequest(
                request.getGroupId(), 
                request.getRequestId(), 
                request.getApproved());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> getGroupMembers(@PathVariable String groupId) {
        List<GroupMemberResponse> response = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/members")
    public ResponseEntity<GroupMemberResponse> updateMemberRole(
            @RequestBody UpdateMemberRoleRequest request) {
        GroupMemberResponse response = groupService.updateMemberRole(request.getGroupId(), request.getMemberId(), request.getRole());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/members")
    public ResponseEntity<RemoveMemberResponse> removeMember(
            @RequestBody RemoveMemberRequest request) {
        RemoveMemberResponse response = groupService.removeMember(request.getGroupId(), request.getMemberId());
        return ResponseEntity.ok(response);
    }
}
