package com.socialapp.profileservice.repository;

import com.socialapp.profileservice.entity.UserProfile;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface UserProfileRepository extends Neo4jRepository<UserProfile, String> {
    Optional<UserProfile> findByUserId(String userId);

     @Query("""
        MATCH (u:user_profile {userId: $userId})-[r:FRIEND_WITH]-(f:user_profile)
        WHERE r.status = 'ACCEPTED'
        RETURN f
        SKIP $skip LIMIT $limit
    """)
    List<UserProfile> findFriendsByUserId(String userId, long skip, long limit);

    // Lời mời đã gửi
    @Query("""
        MATCH (me:user_profile {userId: $userId})-[r:FRIEND_WITH]->(receiver:user_profile)
        WHERE r.status = 'PENDING'
        RETURN receiver
        SKIP $skip LIMIT $limit
    """)
    List<UserProfile> findSentFriendRequests(String userId, long skip, long limit);

    // Lời mời đã nhận
    @Query("""
        MATCH (sender:user_profile)-[r:FRIEND_WITH]->(me:user_profile {userId: $userId})
        WHERE r.status = 'PENDING'
        RETURN sender
        SKIP $skip LIMIT $limit
        """)
    List<UserProfile> findReceivedFriendRequests(String userId, long skip, long limit);
}
