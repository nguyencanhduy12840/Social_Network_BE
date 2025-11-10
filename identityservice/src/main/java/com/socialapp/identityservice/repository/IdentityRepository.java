package com.socialapp.identityservice.repository;

import com.socialapp.identityservice.entity.Identity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentityRepository extends JpaRepository<Identity, String> {
    Identity findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Identity> findByGoogleId(String googleId);
}
