package com.socialapp.postservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {
    String id;

    String postId;

    String content;

    List<String> media;

    Instant createdAt;

    Instant updatedAt;

    String parentCommentId;

    List<String> likes = new ArrayList<>();

    OneUserProfileResponse.UserProfileOne authorProfile;

}
