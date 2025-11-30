package com.socialapp.postservice.dto.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreatePostRequest {
    private String userId;
    private String content;
    private List<String> media;
    private String groupId;
    private String privacy;
    private String type;
}
