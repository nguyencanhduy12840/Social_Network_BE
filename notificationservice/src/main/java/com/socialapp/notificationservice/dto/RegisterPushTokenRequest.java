package com.socialapp.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPushTokenRequest {
    @NotBlank(message = "Token is required")
    private String token;

    private String type; // ios, android
    private String deviceId;
    private String deviceName;
}
