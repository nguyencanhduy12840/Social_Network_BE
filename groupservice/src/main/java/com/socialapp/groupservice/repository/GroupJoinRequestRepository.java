package com.socialapp.groupservice.repository;

import com.socialapp.groupservice.entity.GroupJoinRequest;
import com.socialapp.groupservice.util.constant.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, String> {

    @Query("SELECT gjr FROM GroupJoinRequest gjr WHERE gjr.group.id = :groupId AND gjr.userId = :userId")
    Optional<GroupJoinRequest> findByGroupIdAndUserId(@Param("groupId") String groupId, @Param("userId") String userId);

    @Query("SELECT gjr FROM GroupJoinRequest gjr WHERE gjr.group.id = :groupId AND gjr.userId = :userId AND gjr.status = :status")
    Optional<GroupJoinRequest> findByGroupIdAndUserIdAndStatus(
            @Param("groupId") String groupId,
            @Param("userId") String userId,
            @Param("status") JoinRequestStatus status);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM GroupJoinRequest gjr WHERE gjr.group.id = :groupId")
    void deleteAllByGroupId(@Param("groupId") String groupId);

    @Query("SELECT gjr FROM GroupJoinRequest gjr WHERE gjr.userId = :userId AND gjr.status = :status")
    java.util.List<GroupJoinRequest> findAllByUserIdAndStatus(@Param("userId") String userId, @Param("status") JoinRequestStatus status);
}

