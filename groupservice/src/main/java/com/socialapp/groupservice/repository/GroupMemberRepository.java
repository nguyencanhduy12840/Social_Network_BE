package com.socialapp.groupservice.repository;

import com.socialapp.groupservice.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, String> {

    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.id = :groupId")
    Integer countMembersByGroupId(@Param("groupId") String groupId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.userId = :userId")
    Optional<GroupMember> findByGroupIdAndUserId(@Param("groupId") String groupId, @Param("userId") String userId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId")
    List<GroupMember> findAllByGroupId(@Param("groupId") String groupId);

    @Query("DELETE FROM GroupMember gm WHERE gm.group.id = :groupId")
    void deleteAllByGroupId(@Param("groupId") String groupId);
}
