package com.socialapp.chatservice.dto.request;

import lombok.*;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MarkAsReadRequest {
    @NotBlank(message = "Chat ID không được để trống")
    private String chatId;
}

