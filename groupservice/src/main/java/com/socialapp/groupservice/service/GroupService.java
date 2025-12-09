package com.socialapp.groupservice.service;

import com.socialapp.groupservice.dto.request.CreateGroupRequest;
import com.socialapp.groupservice.dto.request.UpdateGroupRequest;
import com.socialapp.groupservice.dto.response.*;
import com.socialapp.groupservice.entity.Group;
import com.socialapp.groupservice.entity.GroupJoinRequest;
import com.socialapp.groupservice.entity.GroupMember;
import com.socialapp.groupservice.mapper.GroupConverter;
import com.socialapp.groupservice.repository.GroupRepository;
import com.socialapp.groupservice.repository.GroupMemberRepository;
import com.socialapp.groupservice.repository.GroupJoinRequestRepository;
import com.socialapp.groupservice.util.SecurityUtil;
import com.socialapp.groupservice.util.constant.GroupRole;
import com.socialapp.groupservice.util.constant.JoinRequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    private final GroupRepository groupRepository;

    private final GroupConverter groupConverter;

    private final CloudinaryService cloudinaryService;

    private final GroupMemberRepository groupMemberRepository;

    private final GroupJoinRequestRepository groupJoinRequestRepository;

    public GroupService(GroupRepository groupRepository, GroupConverter groupConverter,
                        CloudinaryService cloudinaryService, GroupMemberRepository groupMemberRepository,
                        GroupJoinRequestRepository groupJoinRequestRepository) {
        this.cloudinaryService = cloudinaryService;
        this.groupConverter = groupConverter;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupJoinRequestRepository = groupJoinRequestRepository;
    }

    @Transactional
    public CreateGroupResponse createGroup(CreateGroupRequest request, MultipartFile backgroundImage) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tạo group entity
        Group group = new Group();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setOwnerId(currentUserId);

        // Upload background image nếu có
        if (backgroundImage != null && !backgroundImage.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(backgroundImage);
            group.setBackgroundImageUrl(imageUrl);
        }
        // Lưu group vào database
        Group savedGroup = groupRepository.save(group);

        // Convert sang response DTO sử dụng ModelMapper
        return groupConverter.toCreateGroupResponse(savedGroup);
    }

    @Transactional(readOnly = true)
    public GroupDetailResponse getGroupDetail(String groupId) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin().orElse(null);

        // Tìm group theo ID
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Convert sang response DTO sử dụng ModelMapper
        GroupDetailResponse response = groupConverter.toGroupDetailResponse(group);

        // Đếm số lượng thành viên
        Integer memberCount = groupMemberRepository.countMembersByGroupId(groupId);
        response.setMemberCount(memberCount != null ? memberCount : 0);

        // Kiểm tra người dùng hiện tại có phải là chủ nhóm không
        response.setIsOwner(currentUserId != null && currentUserId.equals(group.getOwnerId()));

        // Kiểm tra người dùng hiện tại có phải là thành viên không và lấy vai trò
        if (currentUserId != null) {
            groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                    .ifPresentOrElse(
                            member -> {
                                response.setIsMember(true);
                                response.setCurrentUserRole(member.getRole());
                            },
                            () -> {
                                response.setIsMember(false);
                                response.setCurrentUserRole(null);
                            }
                    );
        } else {
            response.setIsMember(false);
            response.setCurrentUserRole(null);
        }

        return response;
    }

    @Transactional
    public JoinGroupResponse joinGroup(String groupId) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm group theo ID
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Kiểm tra người dùng đã là thành viên chưa
        if (groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId).isPresent()) {
            throw new RuntimeException("You are already a member of this group");
        }

        // Kiểm tra đã có yêu cầu pending chưa
        Optional<GroupJoinRequest> existingRequest = groupJoinRequestRepository
                .findByGroupIdAndUserIdAndStatus(groupId, currentUserId, JoinRequestStatus.PENDING);

        if (existingRequest.isPresent()) {
            throw new RuntimeException("You already have a pending join request for this group");
        }

        // Tạo join request
        GroupJoinRequest joinRequest = new GroupJoinRequest();
        joinRequest.setGroup(group);
        joinRequest.setUserId((currentUserId));
        joinRequest.setStatus(JoinRequestStatus.PENDING);

        GroupJoinRequest savedRequest = groupJoinRequestRepository.save(joinRequest);

        // Tạo response
        JoinGroupResponse response = new JoinGroupResponse();
        response.setId(savedRequest.getId());
        response.setGroupId(group.getId());
        response.setGroupName(group.getName());
        response.setUserId(currentUserId);
        response.setStatus(savedRequest.getStatus());
        response.setRequestedAt(savedRequest.getRequestedAt());
        response.setMessage("Join request sent successfully. Waiting for approval.");

        return response;
    }

    @Transactional
    public LeaveGroupResponse leaveGroup(String groupId) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm group theo ID
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Kiểm tra người dùng có phải là chủ nhóm không
        if (group.getOwnerId().equals(currentUserId)) {
            throw new RuntimeException("Owner cannot leave the group. Please transfer ownership or delete the group.");
        }

        // Tìm membership của người dùng
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        // Xóa membership
        groupMemberRepository.delete(member);

        // Tạo response
        LeaveGroupResponse response = new LeaveGroupResponse();
        response.setGroupId(group.getId());
        response.setGroupName(group.getName());
        response.setUserId(currentUserId);
        response.setLeftAt(Instant.now());
        response.setMessage("You have successfully left the group.");

        return response;
    }

    @Transactional
    public HandleJoinRequestResponse handleJoinRequest(String groupId, String requestId, Boolean approved) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm group theo ID
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Kiểm tra người dùng có quyền duyệt không (phải là owner hoặc admin)
        boolean isOwner = group.getOwnerId().equals(currentUserId);
        boolean isAdmin = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .map(member -> member.getRole() == GroupRole.ADMIN)
                .orElse(false);

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You don't have permission to handle join requests. Only owner or admin can approve/reject members.");
        }

        // Tìm join request
        GroupJoinRequest joinRequest = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Join request not found"));

        // Kiểm tra join request có thuộc group này không
        if (!joinRequest.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Join request does not belong to this group");
        }

        // Kiểm tra trạng thái request
        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new RuntimeException("This join request has already been handled");
        }

        // Xử lý request
        Instant handledAt = Instant.now();
        joinRequest.setHandledAt(handledAt);

        if (approved) {
            // Approve - thêm vào group members
            joinRequest.setStatus(JoinRequestStatus.APPROVED);

            // Tạo group member mới
            GroupMember newMember = new GroupMember();
            newMember.setGroup(group);
            newMember.setUserId(joinRequest.getUserId());
            newMember.setRole(GroupRole.MEMBER); // Mặc định là MEMBER

            groupMemberRepository.save(newMember);
        } else {
            // Reject
            joinRequest.setStatus(JoinRequestStatus.REJECTED);
        }

        groupJoinRequestRepository.save(joinRequest);

        // Tạo response
        HandleJoinRequestResponse response = new HandleJoinRequestResponse();
        response.setRequestId(joinRequest.getId());
        response.setGroupId(group.getId());
        response.setGroupName(group.getName());
        response.setUserId(String.valueOf(joinRequest.getUserId()));
        response.setStatus(joinRequest.getStatus());
        response.setHandledAt(handledAt);
        response.setMessage(approved ?
                "Join request approved successfully. User is now a member." :
                "Join request rejected.");

        return response;
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getGroupMembers(String groupId) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm group theo ID
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Kiểm tra người dùng có quyền xem danh sách thành viên không
        boolean isOwner = group.getOwnerId().equals(currentUserId);
        boolean isMember = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId).isPresent();

        if (!isOwner && !isMember) {
            throw new RuntimeException("You don't have permission to view members. Only members can view the member list.");
        }

        // Lấy danh sách thành viên
        List<GroupMember> members = groupMemberRepository.findAllByGroupId(groupId);

        // Convert sang response DTO
        return members.stream()
                .map(member -> {
                    GroupMemberResponse response = new GroupMemberResponse();
                    response.setId(member.getId());
                    response.setUserId(String.valueOf(member.getUserId()));
                    response.setGroupId(groupId);
                    response.setRole(member.getRole());
                    response.setJoinedAt(member.getJoinedAt());
                    return response;
                })
                .toList();
    }

    @Transactional
    public GroupMemberResponse updateMemberRole(String groupId, String memberId, GroupRole newRole) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm group theo ID
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Kiểm tra người dùng có quyền thay đổi role không (phải là owner hoặc admin)
        boolean isOwner = group.getOwnerId().equals(currentUserId);
        boolean isAdmin = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .map(member -> member.getRole() == GroupRole.ADMIN)
                .orElse(false);

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You don't have permission to update member roles. Only owner or admin can manage members.");
        }

        // Tìm member cần thay đổi role
        GroupMember member = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Kiểm tra member có thuộc group này không
        if (!member.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Member does not belong to this group");
        }

        // Kiểm tra không thể thay đổi role của owner
        if (group.getOwnerId().equals(String.valueOf(member.getUserId()))) {
            throw new RuntimeException("Cannot change the role of the group owner");
        }

        // Cập nhật role
        member.setRole(newRole);
        GroupMember updatedMember = groupMemberRepository.save(member);

        // Tạo response
        GroupMemberResponse response = new GroupMemberResponse();
        response.setId(updatedMember.getId());
        response.setUserId(String.valueOf(updatedMember.getUserId()));
        response.setGroupId(groupId);
        response.setRole(updatedMember.getRole());
        response.setJoinedAt(updatedMember.getJoinedAt());

        return response;
    }

    @Transactional
    public RemoveMemberResponse removeMember(String groupId, String memberId) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm group theo ID
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Kiểm tra người dùng có quyền xóa thành viên không (phải là owner hoặc admin)
        boolean isOwner = group.getOwnerId().equals(currentUserId);
        boolean isAdmin = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .map(member -> member.getRole() == GroupRole.ADMIN)
                .orElse(false);

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You don't have permission to remove members. Only owner or admin can remove members.");
        }

        // Tìm member cần xóa
        GroupMember member = groupMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Kiểm tra member có thuộc group này không
        if (!member.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Member does not belong to this group");
        }

        // Kiểm tra không thể xóa owner
        if (group.getOwnerId().equals(String.valueOf(member.getUserId()))) {
            throw new RuntimeException("Cannot remove the group owner");
        }

        // Kiểm tra admin không thể xóa admin khác (chỉ owner mới có thể)
        if (!isOwner && member.getRole() == GroupRole.ADMIN) {
            throw new RuntimeException("Admin cannot remove another admin. Only owner can remove admins.");
        }

        String removedUserId = String.valueOf(member.getUserId());

        // Xóa member
        groupMemberRepository.delete(member);

        // Tạo response
        RemoveMemberResponse response = new RemoveMemberResponse();
        response.setGroupId(group.getId());
        response.setGroupName(group.getName());
        response.setRemovedUserId(removedUserId);
        response.setRemovedAt(Instant.now());
        response.setMessage("Member removed successfully from the group.");

        return response;
    }

    @Transactional
    public UpdateGroupResponse updateGroup(UpdateGroupRequest request, MultipartFile backgroundImage) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm group theo ID từ request
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Kiểm tra người dùng có phải là chủ nhóm không
        if (!group.getOwnerId().equals(currentUserId)) {
            throw new RuntimeException("Only the group owner can update the group information");
        }

        // Cập nhật thông tin nhóm
        group.setName(request.getName());
        group.setDescription(request.getDescription());

        // Upload background image mới nếu có
        if (backgroundImage != null && !backgroundImage.isEmpty()) {
            // Xóa hình ảnh cũ trên Cloudinary nếu có
            if (group.getBackgroundImageUrl() != null) {
                cloudinaryService.deleteImage(group.getBackgroundImageUrl());
            }
            // Upload hình ảnh mới
            String newImageUrl = cloudinaryService.uploadImage(backgroundImage);
            group.setBackgroundImageUrl(newImageUrl);
        }

        // Lưu thay đổi vào database
        Group updatedGroup = groupRepository.save(group);

        // Convert sang response DTO sử dụng ModelMapper
        return groupConverter.toUpdateGroupResponse(updatedGroup);
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getGroupMembersInternal(String groupId) {
        // Method này dùng cho internal call từ các service khác
        // Không cần kiểm tra authentication

        // Tìm group theo ID
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Lấy danh sách thành viên
        List<GroupMember> members = groupMemberRepository.findAllByGroupId(groupId);

        // Convert sang response DTO
        return members.stream()
                .map(member -> {
                    GroupMemberResponse response = new GroupMemberResponse();
                    response.setId(member.getId());
                    response.setUserId(String.valueOf(member.getUserId()));
                    response.setGroupId(groupId);
                    response.setRole(member.getRole());
                    response.setJoinedAt(member.getJoinedAt());
                    return response;
                })
                .toList();
    }
}
