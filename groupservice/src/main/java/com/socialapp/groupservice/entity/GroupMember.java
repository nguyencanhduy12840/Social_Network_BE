package com.socialapp.groupservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.socialapp.groupservice.util.constant.GroupRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "group_members")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @JsonIgnore
    private Group group;

    private String userId;

    @Enumerated(EnumType.STRING)
    private GroupRole role;

    private Instant joinedAt;

    @PrePersist
    public void prePersist() {
        joinedAt = Instant.now();
    }

}
