package com.socialapp.profileservice.repository;

import com.socialapp.profileservice.entity.UserProfile;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface UserProfileRepository extends Neo4jRepository<UserProfile, String> {
}
