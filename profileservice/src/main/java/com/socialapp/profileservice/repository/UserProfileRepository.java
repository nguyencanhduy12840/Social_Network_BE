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
        MATCH (a:user_profile {userId:$senderId}), (b:user_profile {userId:$receiverId})
        CREATE (a)-[:FRIEND_WITH {status:'PENDING', requestedAt:datetime()}]->(b)
        """)
    void createFriendRequest(String senderId, String receiverId);

    @Query("""
        MATCH (a:user_profile {userId:$senderId})-[r:FRIEND_WITH {status:'PENDING'}]->(b:user_profile {userId:$receiverId})
        SET r.status = 'ACCEPTED', r.since = datetime()
        """)
    void acceptFriendship(String senderId, String receiverId);

    @Query("""
        MATCH (a:user_profile {userId:$userId})-[r:FRIEND_WITH]-(b:user_profile {userId:$friendUserId})
        DELETE r
        """)
    void deleteFriendshipBetween(String userId, String friendUserId);

    @Query("""
        MATCH (a:user_profile {userId:$aId})-[r:FRIEND_WITH]-(b:user_profile {userId:$bId})
        WHERE r.status = 'ACCEPTED'
        RETURN COUNT(r) > 0
        """)
    boolean hasFriendshipBetween(String aId, String bId);

    // Sent requests
    @Query("""
        MATCH (u:user_profile {userId:$userId})-[:FRIEND_WITH {status:'PENDING'}]->(friend:user_profile)
        RETURN friend
        SKIP $skip LIMIT $size
        """)
    List<UserProfile> findSentFriendRequests(String userId, long skip, long size);

    // Received requests
    @Query("""
        MATCH (u:user_profile {userId:$userId})<-[:FRIEND_WITH {status:'PENDING'}]-(friend:user_profile)
        RETURN friend
        SKIP $skip LIMIT $size
        """)
    List<UserProfile> findReceivedFriendRequests(String userId, long skip, long size);

    // Friends
    @Query("""
        MATCH (u:user_profile {userId:$userId})-[:FRIEND_WITH {status:'ACCEPTED'}]-(friend:user_profile)
        RETURN DISTINCT friend
        SKIP $skip LIMIT $size
        """)
    List<UserProfile> findFriendsByUserId(String userId, long skip, long size);
    @Query("""
        MATCH (u:user_profile {userId:$userId})-[:FRIEND_WITH {status:'ACCEPTED'}]-(friend:user_profile)
        RETURN COUNT(friend)
        """)
    long countFriendsByUserId(String userId);
}
