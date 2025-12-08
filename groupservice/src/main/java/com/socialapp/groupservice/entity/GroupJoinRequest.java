package com.socialapp.groupservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.socialapp.groupservice.util.constant.JoinRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "group_join_requests")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupJoinRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @JsonIgnore
    private Group group;

    private String userId;

    @Enumerated(EnumType.STRING)
    private JoinRequestStatus status;

    private Instant requestedAt;
    private Instant handledAt;

    @PrePersist
    public void prePersist() {
        requestedAt = Instant.now();
    }
}
