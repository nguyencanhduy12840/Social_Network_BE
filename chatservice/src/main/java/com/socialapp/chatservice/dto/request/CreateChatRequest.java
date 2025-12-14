package com.socialapp.chatservice.dto.request;

import lombok.*;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateChatRequest {
    @NotBlank(message = "Recipient ID không được để trống")
    private String recipientId; // ID của người nhận
}

