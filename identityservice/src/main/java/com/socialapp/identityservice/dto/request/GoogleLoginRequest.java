package com.socialapp.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GoogleLoginRequest {
    @NotBlank(message = "ID token is required")
    private String idToken;
}
