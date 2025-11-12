package com.socialapp.profileservice.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import com.socialapp.profileservice.util.FriendshipStatus;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RelationshipProperties
public class Friendship {

    @Id
    @GeneratedValue
    Long id;

    FriendshipStatus status;     // PENDING, ACCEPTED, REJECTED
    String direction;            // OUTGOING / INCOMING
    Instant requestedAt;
    Instant since;

    @TargetNode
    UserProfile friend;
}
