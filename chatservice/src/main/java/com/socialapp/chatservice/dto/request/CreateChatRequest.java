package com.socialapp.chatservice.dto.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateChatRequest {
    private String recipientId;
}
