package com.socialapp.profileservice.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Node("user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;

    @Property("userId")
    String userId;

    String email;
    String firstName;
    String lastName;
    String username;
    String avatarUrl;
    String bio;
    String gender;
    LocalDate dob;

    // Người này gửi lời mời
    @Relationship(type = "FRIEND_WITH", direction = Relationship.Direction.OUTGOING)
    Set<Friendship> sentFriendships = new HashSet<>();

    // Người này nhận lời mời
    @Relationship(type = "FRIEND_WITH", direction = Relationship.Direction.INCOMING)
    Set<Friendship> receivedFriendships = new HashSet<>();
}
