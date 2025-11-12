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
        CREATE (a)-[:FRIEND_WITH {status:'PENDING', direction:'OUTGOING', requestedAt:datetime()}]->(b),
               (b)-[:FRIEND_WITH {status:'PENDING', direction:'INCOMING', requestedAt:datetime()}]->(a)
        """)
    void createFriendRequest(String senderId, String receiverId);

    @Query("""
        MATCH (a:user_profile {userId:$userId})-[r:FRIEND_WITH]-(b:user_profile {userId:$friendUserId})
        DELETE r
        """)
    void deleteFriendshipBetween(String userId, String friendUserId);

    @Query("""
        MATCH (a:user_profile {userId:$senderId})-[r:FRIEND_WITH]-(b:user_profile {userId:$receiverId})
        SET r.status = 'ACCEPTED', r.since = datetime()
        """)
    void acceptFriendship(String senderId, String receiverId);

    @Query("""
        MATCH (a:user_profile {userId:$aId})-[r:FRIEND_WITH]-(b:user_profile {userId:$bId})
        RETURN COUNT(r) > 0
        """)
    boolean hasFriendshipBetween(String aId, String bId);

    @Query("""
        MATCH (u:UserProfile {userId: $userId})-[:FRIEND_WITH {status: 'ACCEPTED'}]-(friend:UserProfile)
        RETURN DISTINCT friend
        SKIP $skip LIMIT $size
        """)
    List<UserProfile> findFriendsByUserId(String userId, long skip, int size);

    @Query("""
        MATCH (u:UserProfile {userId: $userId})-[r:FRIEND_WITH {status: 'PENDING', direction: 'OUTGOING'}]->(friend:UserProfile)
        RETURN DISTINCT friend
        SKIP $skip LIMIT $size
        """)
    List<UserProfile> findSentFriendRequests(String userId, long skip, int size);

    @Query("""
        MATCH (u:UserProfile {userId: $userId})<-[r:FRIEND_WITH {status: 'PENDING', direction: 'INCOMING'}]-(friend:UserProfile)
        RETURN DISTINCT friend
        SKIP $skip LIMIT $size
        """)
    List<UserProfile> findReceivedFriendRequests(String userId, long skip, int size);
}
