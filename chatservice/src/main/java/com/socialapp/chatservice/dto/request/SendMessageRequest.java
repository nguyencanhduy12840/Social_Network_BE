package com.socialapp.chatservice.dto.request;

import com.socialapp.chatservice.entity.Message;
import lombok.*;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SendMessageRequest {
    @NotBlank(message = "Chat ID không được để trống")
    private String chatId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;

    @Builder.Default
    private Message.MessageType type = Message.MessageType.TEXT;

    private String fileUrl; // URL file nếu là tin nhắn file/image

    private String fileName; // Tên file
}
