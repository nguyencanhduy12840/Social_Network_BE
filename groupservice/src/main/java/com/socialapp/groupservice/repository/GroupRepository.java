package com.socialapp.groupservice.repository;

import com.socialapp.groupservice.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, String> {
    // Search groups by name
    List<Group> findByNameContainingIgnoreCase(String name);
}
