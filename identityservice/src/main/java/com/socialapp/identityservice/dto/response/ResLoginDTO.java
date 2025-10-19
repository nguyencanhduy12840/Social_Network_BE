package com.socialapp.identityservice.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResLoginDTO {
    private String id;

    private String email;

    private String accessToken;

    private String refreshToken;
}
