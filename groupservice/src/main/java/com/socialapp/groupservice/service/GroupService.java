package com.socialapp.groupservice.service;

import com.socialapp.groupservice.client.PostClient;
import com.socialapp.groupservice.client.ProfileClient;
import com.socialapp.groupservice.dto.request.*;
import com.socialapp.groupservice.dto.response.*;
import com.socialapp.groupservice.entity.Group;
import com.socialapp.groupservice.entity.GroupJoinRequest;
import com.socialapp.groupservice.entity.GroupMember;
import com.socialapp.groupservice.mapper.GroupConverter;
import com.socialapp.groupservice.repository.GroupRepository;
import com.socialapp.groupservice.repository.GroupMemberRepository;
import com.socialapp.groupservice.repository.GroupJoinRequestRepository;
import com.socialapp.groupservice.util.SecurityUtil;
import com.socialapp.groupservice.util.constant.GroupPrivacy;
import com.socialapp.groupservice.util.constant.GroupRole;
import com.socialapp.groupservice.util.constant.JoinRequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    private final GroupRepository groupRepository;

    private final GroupConverter groupConverter;

    private final CloudinaryService cloudinaryService;

    private final GroupMemberRepository groupMemberRepository;

    private final GroupJoinRequestRepository groupJoinRequestRepository;
    
    private final ProfileClient profileClient;
    
    private final PostClient postClient;

    public GroupService(GroupRepository groupRepository, GroupConverter groupConverter,
                        CloudinaryService cloudinaryService, GroupMemberRepository groupMemberRepository,
            GroupJoinRequestRepository groupJoinRequestRepository, ProfileClient profileClient,
            PostClient postClient) {
        this.cloudinaryService = cloudinaryService;
        this.groupConverter = groupConverter;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupJoinRequestRepository = groupJoinRequestRepository;
        this.profileClient = profileClient;
        this.postClient = postClient;
    }
    
     private UserResponse buildUserResponse(String userId) {
        try {
            OneUserProfileResponse response = profileClient.getUserProfile(userId);
            if (response != null && response.getData() != null) {
                OneUserProfileResponse.UserProfileOne profile = response.getData();
                return new UserResponse(
                    profile.getId(),
                    profile.getUsername(),
                    profile.getAvatarUrl()
                );
            }
        } catch (Exception e) {
            System.err.println("Error fetching profile for user " + userId + ": " + e.getMessage());
        }
        return new UserResponse(userId, "Unknown User", null);
    }
    
    private GroupResponse buildGroupResponse(Group group) {
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

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, MultipartFile background, MultipartFile avatar) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tạo group entity
        Group group = new Group();
        group.setOwnerId(currentUserId);
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setPrivacy(request.getPrivacy());
        
        // Upload background image
        if (background != null && !background.isEmpty()) {
            String imageUrl = cloudinaryService.uploadImage(background);
            group.setBackgroundUrl(imageUrl);
        }
        
        // Upload avatar
        if (avatar != null && !avatar.isEmpty()) {
            String avatarUrl = cloudinaryService.uploadImage(avatar);
            group.setAvatarUrl(avatarUrl);
        }

        // Lưu group vào database
        Group savedGroup = groupRepository.save(group);

        // Add Owner as a Group Member with OWNER role
        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroup(savedGroup);
        ownerMember.setUserId(currentUserId);
        ownerMember.setRole(GroupRole.OWNER);
        groupMemberRepository.save(ownerMember);

        return groupConverter.toGroupResponse(savedGroup);
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

        Integer memberCount = groupMemberRepository.countMembersByGroupId(groupId);
        response.setMemberCount(memberCount != null ? memberCount : 0);

        response.setAvatarUrl(group.getAvatarUrl());
        response.setBackgroundUrl(group.getBackgroundUrl());
        response.setPrivacy(GroupPrivacy.valueOf(group.getPrivacy().name()));

        if (currentUserId != null) {
            Optional<GroupMember> memberOpt = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId);
            if (memberOpt.isPresent()) {
                response.setRole(memberOpt.get().getRole());
                response.setJoinStatus(null);
            } else {
                response.setRole(null);
                 Optional<GroupJoinRequest> requestOpt = groupJoinRequestRepository
                         .findByGroupIdAndUserIdAndStatus(groupId, currentUserId, JoinRequestStatus.PENDING);
                 if (requestOpt.isPresent()) {
                     response.setJoinStatus(JoinRequestStatus.PENDING);
                 } else {
                     response.setJoinStatus(null);
                 }
            }
        } else {
            response.setRole(null);
            response.setJoinStatus(null);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getAllGroups() {
        List<Group> groups = groupRepository.findAll();
        return groups.stream()
                .map(this::buildGroupResponse)
                .toList();
    }
    
    @Transactional
    public RequestResponse joinGroup(String groupId) {
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
        
        // Check pending requests
        if (groupJoinRequestRepository.findByGroupIdAndUserIdAndStatus(groupId, currentUserId, JoinRequestStatus.PENDING).isPresent()) {
             throw new RuntimeException("You already have a pending join request for this group");
        }

        // Nếu Group là PUBLIC -> Join luôn
        if (group.getPrivacy() == GroupPrivacy.PUBLIC) {
             GroupMember newMember = new GroupMember();
             newMember.setGroup(group);
             newMember.setUserId(currentUserId);
             newMember.setRole(GroupRole.MEMBER);
             GroupMember savedMember = groupMemberRepository.save(newMember);
             
            RequestResponse response = new RequestResponse();
            response.setId(savedMember.getId());
            response.setUser(buildUserResponse(currentUserId));
            response.setGroup(buildGroupResponse(group));
            response.setStatus(JoinRequestStatus.APPROVED);
            return response;
        } 
        
        // Nếu Group là PRIVATE -> Tạo Join Request
        GroupJoinRequest joinRequest = new GroupJoinRequest();
        joinRequest.setGroup(group);
        joinRequest.setUserId((currentUserId));
        joinRequest.setStatus(JoinRequestStatus.PENDING);

        GroupJoinRequest savedRequest = groupJoinRequestRepository.save(joinRequest);

        // Tạo response
        RequestResponse response = new RequestResponse();
        response.setId(savedRequest.getId());
        response.setUser(buildUserResponse(currentUserId));
        response.setGroup(buildGroupResponse(group));
        response.setStatus(savedRequest.getStatus());

        return response;
    }

    @Transactional
    public String leaveGroup(String groupId) {
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

        // Xóa tất cả posts của member này trong group
        try {
            postClient.deletePostsByGroupIdAndAuthorId(groupId, currentUserId);
        } catch (Exception e) {
            System.err.println("Error deleting posts for member leaving group: " + e.getMessage());
        }

        // Xóa tất cả join requests của user này cho group này (cleanup)
        List<GroupJoinRequest> userRequests = groupJoinRequestRepository
                .findAllByGroupIdAndUserId(groupId, currentUserId);
        if (!userRequests.isEmpty()) {
            groupJoinRequestRepository.deleteAll(userRequests);
        }

        // Xóa membership
        groupMemberRepository.delete(member);

        return "Successfully left the group";
    }

    @Transactional
    public String cancelJoinRequest(String groupId) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm pending request của user cho group này
        GroupJoinRequest joinRequest = groupJoinRequestRepository
                .findByGroupIdAndUserIdAndStatus(groupId, currentUserId, JoinRequestStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("No pending join request found for this group"));

        // Xóa join request
        groupJoinRequestRepository.delete(joinRequest);

        return "Join request cancelled successfully";
    }

    @Transactional
    public RequestResponse handleJoinRequest(String requestId, Boolean approved) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm join request
        GroupJoinRequest joinRequest = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Join request not found"));

        // Lấy group từ joinRequest
        Group group = joinRequest.getGroup();
        String groupId = group.getId();

        // Kiểm tra trạng thái request
        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new RuntimeException("This join request has already been handled");
        }

        // Kiểm tra người dùng có quyền duyệt không (phải là owner hoặc admin)
        boolean isOwner = group.getOwnerId().equals(currentUserId);
        boolean isAdmin = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .map(member -> member.getRole() == GroupRole.ADMIN)
                .orElse(false);

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You don't have permission to handle join requests. Only owner or admin can approve/reject members.");
        }

        // Xử lý request
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
        RequestResponse response = new RequestResponse();
        response.setId(joinRequest.getId());
        response.setUser(buildUserResponse(joinRequest.getUserId()));
        response.setGroup(buildGroupResponse(group));
        response.setStatus(joinRequest.getStatus());
        return response;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getGroupMembers(String groupId) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm group theo ID
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        boolean isPublic = group.getPrivacy() == GroupPrivacy.PUBLIC;
        boolean isOwner = group.getOwnerId().equals(currentUserId);
        boolean isMember = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId).isPresent();

        if (!isPublic && !isOwner && !isMember) {
            throw new RuntimeException("This group is private. Only members can view the member list.");
        }

        // Lấy danh sách thành viên
        List<GroupMember> members = groupMemberRepository.findAllByGroupId(groupId);

        // Convert sang response DTO
        return members.stream()
                .map(member -> {
                    MemberResponse response = new MemberResponse();
                    response.setUser(buildUserResponse(member.getUserId()));
                    response.setRole(member.getRole());
                    return response;
                })
                .toList();
    }

    @Transactional
    public MemberResponse updateMemberRole(String groupId, String memberId, GroupRole newRole) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm member thực hiện hành động (actor)
        GroupMember actor = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        boolean isOwner = actor.getRole() == GroupRole.OWNER;
        boolean isAdmin = actor.getRole() == GroupRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You don't have permission to update member roles.");
        }

        // Tìm member cần thay đổi role (target)
        GroupMember targetMember = groupMemberRepository.findByGroupIdAndUserId(groupId, memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (!targetMember.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Member does not belong to this group");
        }

        // Strict Permission Logic:
        // Owner can update anyone (except logic usually prevents changing self role if it leaves group ownerless, but here simpler).
        // Admin can ONLY update MEMBER. Target CANNOT be ADMIN or OWNER.
        
        if (targetMember.getRole() == GroupRole.OWNER) {
             throw new RuntimeException("Cannot change the role of the group owner");
        }

        if (isAdmin) {
            if (targetMember.getRole() == GroupRole.ADMIN) {
                throw new RuntimeException("Admins cannot update other Admins.");
            }
            // Admin can only interact with Members.
            // Also ensure newRole is not upgrading to Owner/Admin? 
            // "Admin có thể update role thành viên" -> Usually implies managing members within their rank?
            // If Admin promotes Member to Admin -> They become peers.
            // If Admin promotes Member to Owner -> Only Owner can transfer ownership.
            
            if (newRole == GroupRole.OWNER) {
                throw new RuntimeException("Only Owner can assign Owner role.");
            }
            // Let's allow Admin to promote Member to Admin if business allows? 
            // User said: "không thể delete và update nhau". 
            // If target is Member, Admin can update.
        }

        // Kiểm tra validation role
        if (newRole != GroupRole.ADMIN && newRole != GroupRole.MEMBER) {
            throw new RuntimeException("Invalid role. Only ADMIN or MEMBER roles can be assigned.");
        }

        // Cập nhật role
        targetMember.setRole(newRole);
        GroupMember updatedMember = groupMemberRepository.save(targetMember);

        MemberResponse response = new MemberResponse();
        response.setUser(buildUserResponse(updatedMember.getUserId()));
        response.setRole(updatedMember.getRole());

        return response;
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> getGroupJoinRequests(String groupId) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
        
        // Tìm group theo ID
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Kiểm tra permission: Chỉ Owner hoặc Admin mới được xem request
        boolean isOwner = group.getOwnerId().equals(currentUserId);
        boolean isAdmin = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .map(member -> member.getRole() == GroupRole.ADMIN)
                .orElse(false);

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("You don't have permission to view join requests.");
        }

        // Lấy danh sách PENDING requests
        List<GroupJoinRequest> requests = groupJoinRequestRepository.findAllByGroupIdAndStatus(groupId, JoinRequestStatus.PENDING);

        // Convert sang response DTO
        return requests.stream()
                .map(req -> {
                    RequestResponse response = new RequestResponse();
                    response.setId(req.getId());
                    response.setUser(buildUserResponse(req.getUserId()));
                    response.setGroup(buildGroupResponse(group));
                    response.setStatus(req.getStatus());
                    return response;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> getMyPendingRequests() {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Lấy tất cả pending requests của user hiện tại
        List<GroupJoinRequest> requests = groupJoinRequestRepository
                .findAllByUserIdAndStatus(currentUserId, JoinRequestStatus.PENDING);

        // Convert sang response DTO với thông tin group
        return requests.stream()
                .map(req -> {
                    Group group = req.getGroup();
                    RequestResponse response = new RequestResponse();
                    response.setId(req.getId());
                    response.setUser(buildUserResponse(currentUserId));
                    response.setGroup(buildGroupResponse(group));
                    response.setStatus(req.getStatus());
                    return response;
                })
                .toList();
    }
    
    @Transactional
    public String removeMember(String groupId, String memberId) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm member thực hiện hành động (actor)
        GroupMember actor = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        boolean isOwner = actor.getRole() == GroupRole.OWNER;
        boolean isAdmin = actor.getRole() == GroupRole.ADMIN;

        if (!isOwner && !isAdmin) {
             throw new RuntimeException("You don't have permission to remove members.");
        }

        GroupMember targetMember = groupMemberRepository.findByGroupIdAndUserId(groupId, memberId)
                        .orElseThrow(() -> new RuntimeException("Member not found"));

        if (!targetMember.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Member does not belong to this group");
        }

        // Strict Permission Check
        if (targetMember.getRole() == GroupRole.OWNER) {
            throw new RuntimeException("Cannot remove the group owner");
        }

        if (isAdmin) {
            // Admin cannot remove Admin
            if (targetMember.getRole() == GroupRole.ADMIN) {
                throw new RuntimeException("Admins cannot remove other Admins.");
            }
            // Admin can only remove Members
        }

        // Xóa tất cả posts của member này trong group
        try {
            postClient.deletePostsByGroupIdAndAuthorId(groupId, memberId);
        } catch (Exception e) {
            System.err.println("Error deleting posts for removed member: " + e.getMessage());
        }

        // Xóa tất cả join requests của member này cho group này (nếu có)
        List<GroupJoinRequest> pendingRequests = groupJoinRequestRepository
                .findAllByGroupIdAndUserId(groupId, memberId);
        if (!pendingRequests.isEmpty()) {
            groupJoinRequestRepository.deleteAll(pendingRequests);
        }
        
        // Xóa member
        groupMemberRepository.delete(targetMember);

        return "Member removed successfully";
    }

    @Transactional
    public void deleteGroup(String groupId) {
        // Lấy userId từ SecurityContext
        String currentUserId = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));

        // Tìm group theo ID
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Kiểm tra người dùng có phải là chủ nhóm không
        if (!group.getOwnerId().equals(currentUserId)) {
            throw new RuntimeException("Only the group owner can delete the group");
        }

        // Xóa tất cả posts trong group
        try {
            postClient.deletePostsByGroupId(groupId);
        } catch (Exception e) {
            System.err.println("Error deleting posts for deleted group: " + e.getMessage());
        }

        // Xóa tất cả members
        groupMemberRepository.deleteAllByGroupId(groupId);
        
        // Xóa tất cả join requests
        groupJoinRequestRepository.deleteAllByGroupId(groupId);
        
        groupRepository.delete(group);
    }

    @Transactional
    public GroupResponse updateGroup(UpdateGroupRequest request, MultipartFile background, MultipartFile avatar) {
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
        group.setPrivacy(request.getPrivacy());

        // Upload background image mới nếu có
        if (background != null && !background.isEmpty()) {
            String newImageUrl = cloudinaryService.uploadImage(background);
            group.setBackgroundUrl(newImageUrl);
        }
        
         // Upload avatar mới nếu có
        if (avatar != null && !avatar.isEmpty()) {
            String newAvatarUrl = cloudinaryService.uploadImage(avatar);
            group.setAvatarUrl(newAvatarUrl);
        }

        // Lưu thay đổi vào database
        Group updatedGroup = groupRepository.save(group);

        return groupConverter.toGroupResponse(updatedGroup);
    }
    
    @Transactional(readOnly = true)
    public List<MemberResponse> getGroupMembersInternal(String groupId) {
        // Method này dùng cho internal call từ các service khác
        // Không cần kiểm tra authentication
        // Lấy danh sách thành viên
        List<GroupMember> members = groupMemberRepository.findAllByGroupId(groupId);

        // Convert sang response DTO
        return members.stream()
                .map(member -> {
                    MemberResponse response = new MemberResponse();
                    response.setUser(buildUserResponse(member.getUserId()));
                    response.setRole(member.getRole());
                    return response;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getJoinedGroups(String targetUserId) {
        List<GroupMember> targetMemberships = groupMemberRepository.findAllByUserId(targetUserId);
        return targetMemberships.stream()
                .map(member -> buildGroupResponse(member.getGroup()))
                .toList();
    }
    
    @Transactional(readOnly = true)
    public boolean isGroupMember(String groupId, String userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId).isPresent();
    }

    @Transactional(readOnly = true)
    public String getGroupPrivacyInternal(String groupId) {
        return groupRepository.findById(groupId)
                .map(g -> g.getPrivacy().name())
                .orElse(null);
    }
    
    @Transactional(readOnly = true)
    public int getGroupCountByUserId(String userId) {
        return groupMemberRepository.findAllByUserId(userId).size();
    }
}
