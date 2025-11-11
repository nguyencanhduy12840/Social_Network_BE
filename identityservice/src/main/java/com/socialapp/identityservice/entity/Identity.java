package com.socialapp.identityservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Identity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String password;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String refreshToken;

    @Column(unique = true)
    private String googleId;

    @Column(length = 20)
    private String authProvider = "email";

    @Column(length = 500)
    private String avatarUrl;

    private LocalDateTime lastLogin;

    boolean isEmailVerified;
}
