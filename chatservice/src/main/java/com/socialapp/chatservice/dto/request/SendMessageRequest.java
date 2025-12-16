package com.socialapp.chatservice.dto.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendMessageRequest {
    private String chatId;
    private String content;
}
