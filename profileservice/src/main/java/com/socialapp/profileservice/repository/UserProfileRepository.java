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
        MATCH (u:user_profile {userId: $userId})-[:FRIEND_WITH]-(f:user_profile)
        WHERE EXISTS((u)-[r:FRIEND_WITH]-(f)) AND r.status = 'ACCEPTED'
        RETURN f
        SKIP $skip LIMIT $limit
    """)
    List<UserProfile> findFriendsByUserId(String userId, long skip, long limit);
}
